package org.folio.circulationbff.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.oneOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.folio.circulationbff.domain.dto.BatchRequest;
import org.folio.circulationbff.domain.dto.BatchRequestCollectionResponse;
import org.folio.circulationbff.domain.dto.BatchRequestDetailsResponse;
import org.folio.circulationbff.domain.dto.BatchRequestResponse;
import org.folio.circulationbff.domain.dto.BffSearchInstance;
import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.folio.circulationbff.domain.dto.PickSlipCollection;
import org.folio.circulationbff.domain.dto.Requests;
import org.folio.circulationbff.domain.dto.SearchSlipCollection;
import org.folio.circulationbff.domain.dto.StaffSlip;
import org.folio.circulationbff.domain.dto.User;
import org.folio.circulationbff.domain.dto.UserCollection;
import org.folio.circulationbff.service.CirculationBffService;
import org.folio.circulationbff.service.MediatedBatchRequestService;
import org.folio.circulationbff.service.MediatedRequestsService;
import org.folio.circulationbff.service.SearchService;
import org.folio.circulationbff.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class CirculationBffControllerTest {

  @Mock
  private SearchService searchService;

  @Mock
  private MediatedRequestsService mediatedRequestsService;

  @Mock
  private UserService userService;

  @Mock
  private CirculationBffService circulationBffService;

  @Mock
  private MediatedBatchRequestService mediatedBatchRequestService;

  @InjectMocks
  private CirculationBffController controller;

  @ParameterizedTest
  @MethodSource
  void externalUsersControllerReturnsTheSameUserCollectionAsUserService(List<User> users) {
    UserCollection userCollection = new UserCollection(users, users == null ? 0 : users.size());
    when(userService.getExternalUser(anyString(), anyString())).thenReturn(userCollection);

    ResponseEntity<UserCollection> actual = controller.getExternalUsers(StringUtils.EMPTY,
      StringUtils.EMPTY);

    assertThat(actual.getStatusCode(), is(HttpStatus.OK));
    assertThat(actual.getBody(), is(userCollection));
  }

  static Stream<List<User>> externalUsersControllerReturnsTheSameUserCollectionAsUserService() {
    return Stream.of(null, Collections.emptyList(), List.of(new User()));
  }

  @ParameterizedTest
  @MethodSource("staffSlips")
  void getPickSlipsControllerReturnsSuccessResponseEntity(List<StaffSlip> staffSlips) {
    PickSlipCollection pickSlipsCollection = new PickSlipCollection(
      Objects.isNull(staffSlips) ? 0 : staffSlips.size(), staffSlips);

    when(circulationBffService.fetchPickSlipsByServicePointId(anyString()))
      .thenReturn(pickSlipsCollection);

    ResponseEntity<PickSlipCollection> actual = controller.getPickSlips(StringUtils.EMPTY);

    assertThat(actual.getStatusCode(), is(HttpStatus.OK));
    assertThat(actual.getBody(), is(pickSlipsCollection));
  }

  @ParameterizedTest
  @MethodSource("staffSlips")
  void getSearchSlipsControllerReturnsSuccessResponseEntity(List<StaffSlip> staffSlips) {
    SearchSlipCollection searchSlipCollection = new SearchSlipCollection(
      Objects.isNull(staffSlips) ? 0 : staffSlips.size(), staffSlips);

    when(circulationBffService.fetchSearchSlipsByServicePointId(anyString()))
      .thenReturn(searchSlipCollection);

    ResponseEntity<SearchSlipCollection> actual = controller.getSearchSlips(StringUtils.EMPTY);

    assertThat(actual.getStatusCode(), is(HttpStatus.OK));
    assertThat(actual.getBody(), is(searchSlipCollection));
  }

  private static Stream<List<StaffSlip>> staffSlips() {
    return Stream.of(null, Collections.emptyList(), List.of(new StaffSlip()));
  }

  @Test
  void instanceFoundSuccessfully() {
    String instanceId = UUID.randomUUID().toString();
    String query = "id=="  + instanceId;
    List<BffSearchInstance> mockSearchResponse = List.of(new BffSearchInstance().id(instanceId));

    when(searchService.findInstances(query))
      .thenReturn(mockSearchResponse);

    var response = controller.circulationBffRequestsSearchInstancesGet(query);
    assertThat(response.getStatusCode(), is(HttpStatus.OK));
  }

  @Test
  void instanceSearchReturnsOnlyOneInstance() {
    String instanceId1 = UUID.randomUUID().toString();
    String instanceId2 = UUID.randomUUID().toString();
    String query = "title==The*";

    List<BffSearchInstance> mockSearchResponse = List.of(
      new BffSearchInstance().id(instanceId1),
      new BffSearchInstance().id(instanceId2));

    when(searchService.findInstances(query))
      .thenReturn(mockSearchResponse);

    var response = controller.circulationBffRequestsSearchInstancesGet(query);
    assertThat(response.getStatusCode(), is(HttpStatus.OK));
    assertThat(response.getBody(), notNullValue());
    assertThat(response.getBody().getId(), oneOf(instanceId1, instanceId2));
  }

  @Test
  void shouldCreateNewMediatedRequestAndConfirm() {
    var mediatedRequest = new MediatedRequest();
    mediatedRequest.setId(null);
    when(mediatedRequestsService.saveMediatedRequest(mediatedRequest))
      .thenReturn(new ResponseEntity<>(mediatedRequest, CREATED));
    when(mediatedRequestsService.confirmMediatedRequest(mediatedRequest))
      .thenReturn(new ResponseEntity<>(NO_CONTENT));
    ResponseEntity<MediatedRequest> response = controller.saveAndConfirmMediatedRequest(mediatedRequest);

    assertThat(response.getStatusCode(), is(CREATED));
    assertThat(response.getBody(), is(mediatedRequest));
  }

  @Test
  void shouldFailWithNewRequestCreation() {
    var mediatedRequest = new MediatedRequest();
    mediatedRequest.setId(null);
    when(mediatedRequestsService.saveMediatedRequest(mediatedRequest))
      .thenReturn(new ResponseEntity<>(BAD_REQUEST));
    ResponseEntity<MediatedRequest> response = controller.saveAndConfirmMediatedRequest(
      mediatedRequest);

    assertThat(response.getStatusCode(), is(BAD_REQUEST));
    assertThat(response.getBody(), is(nullValue()));
  }

  @Test
  void shouldUpdateExistingMediatedRequestAndConfirm() {
    var mediatedRequest = new MediatedRequest();
    mediatedRequest.setId(UUID.randomUUID().toString());
    when(mediatedRequestsService.updateMediatedRequest(mediatedRequest))
      .thenReturn(new ResponseEntity<>(NO_CONTENT));
    when(mediatedRequestsService.confirmMediatedRequest(mediatedRequest))
      .thenReturn(new ResponseEntity<>(NO_CONTENT));
    ResponseEntity<MediatedRequest> response = controller.saveAndConfirmMediatedRequest(
      mediatedRequest);

    assertThat(response.getStatusCode(), is(CREATED));
    assertThat(response.getBody(), is(mediatedRequest));
  }

  @Test
  void shouldFailWithExistingRequestUpdate() {
    var mediatedRequest = new MediatedRequest();
    mediatedRequest.setId(UUID.randomUUID().toString());
    when(mediatedRequestsService.updateMediatedRequest(mediatedRequest))
      .thenReturn(new ResponseEntity<>(BAD_REQUEST));
    ResponseEntity<MediatedRequest> response = controller.saveAndConfirmMediatedRequest(mediatedRequest);

    assertThat(response.getStatusCode(), is(BAD_REQUEST));
    assertThat(response.getBody(), is(nullValue()));
  }

  @Test
  void shouldFailIfExistingRequestSuccessfullyUpdatedButNotConfirmed() {
    var mediatedRequest = new MediatedRequest();
    mediatedRequest.setId(UUID.randomUUID().toString());
    when(mediatedRequestsService.updateMediatedRequest(mediatedRequest))
      .thenReturn(new ResponseEntity<>(NO_CONTENT));
    when(mediatedRequestsService.confirmMediatedRequest(mediatedRequest))
      .thenReturn(new ResponseEntity<>(BAD_REQUEST));
    ResponseEntity<MediatedRequest> response = controller.saveAndConfirmMediatedRequest(
      mediatedRequest);

    assertThat(response.getStatusCode(), is(BAD_REQUEST));
    assertThat(response.getBody(), is(nullValue()));
  }

  @Test
  void createBatchRequestReturnsCreatedResponse() {
    var batchRequest = new BatchRequest();
    var batchResponse = new BatchRequestResponse();
    var serviceResponse = new ResponseEntity<>(batchResponse, CREATED);

    when(mediatedBatchRequestService.createMediatedBatchRequest(batchRequest)).thenReturn(serviceResponse);

    var response = controller.createBatchRequest(batchRequest);

    assertThat(response.getStatusCode(), is(CREATED));
    assertThat(response.getBody(), is(batchResponse));
  }

  @Test
  void createBatchRequestReturnsNotFoundResponse() {
    var batchRequest = new BatchRequest();
    var batchResponse = new BatchRequestResponse();
    var serviceResponse = new ResponseEntity<>(batchResponse, NOT_FOUND);

    when(mediatedBatchRequestService.createMediatedBatchRequest(batchRequest)).thenReturn(serviceResponse);

    var response = controller.createBatchRequest(batchRequest);

    assertThat(response.getStatusCode(), is(NOT_FOUND));
    assertThat(response.getBody(), is(batchResponse));
  }

  @Test
  void getBatchRequestByIdReturnsOkResponse() {
    var batchId = UUID.randomUUID();
    var batchResponse = new BatchRequestResponse();
    var serviceResponse = new ResponseEntity<>(batchResponse, HttpStatus.OK);

    when(mediatedBatchRequestService.retrieveMediatedBatchRequestById(batchId)).thenReturn(serviceResponse);

    var response = controller.getBatchRequestById(batchId);

    assertThat(response.getStatusCode(), is(HttpStatus.OK));
    assertThat(response.getBody(), is(batchResponse));
  }

  @Test
  void getBatchRequestByIdReturnsNotFoundResponse() {
    var batchId = UUID.randomUUID();
    var batchResponse = new BatchRequestResponse();
    var serviceResponse = new ResponseEntity<>(batchResponse, NOT_FOUND);

    when(mediatedBatchRequestService.retrieveMediatedBatchRequestById(batchId)).thenReturn(serviceResponse);

    var response = controller.getBatchRequestById(batchId);

    assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
    assertThat(response.getBody(), is(batchResponse));
  }

  @Test
  void getBatchRequestCollectionReturnsOkResponse() {
    var query = "status==OPEN";
    var offset = 0;
    var limit = 10;
    var batchCollection = new BatchRequestCollectionResponse();

    when(mediatedBatchRequestService.retrieveMediatedBatchRequestsByQuery(query, offset, limit)).thenReturn(batchCollection);

    var response = controller.getBatchRequestCollection(query, offset, limit);

    assertThat(response.getStatusCode(), is(HttpStatus.OK));
    assertThat(response.getBody(), is(batchCollection));
  }

  @Test
  void getMultiItemBatchRequestDetailsByBatchIdReturnsOkResponse() {
    var batchId = UUID.randomUUID();
    var offset = 0;
    var limit = 5;
    var detailsResponse = new BatchRequestDetailsResponse();

    when(mediatedBatchRequestService.retrieveMediatedBatchRequestDetails(batchId, offset, limit)).thenReturn(detailsResponse);

    var response = controller.getMultiItemBatchRequestDetailsByBatchId(batchId, offset, limit);

    assertThat(response.getStatusCode(), is(HttpStatus.OK));
    assertThat(response.getBody(), is(detailsResponse));
  }

  @Test
  void getRequestsEnrichedWithBatchInfoReturnsOkResponse() {
    var totalRecords = "auto";
    var offset = 0;
    var limit = 5;
    var query = "some query";
    var enrichedRequests = new Requests();

    when(circulationBffService.getBatchRequestInfoEnrichedRequests(query, offset, limit, totalRecords))
      .thenReturn(enrichedRequests);

    var response = controller.getRequests(query, limit, offset, totalRecords);

    assertThat(response.getStatusCode(), is(HttpStatus.OK));
    assertThat(response.getBody(), is(enrichedRequests));
  }

}
