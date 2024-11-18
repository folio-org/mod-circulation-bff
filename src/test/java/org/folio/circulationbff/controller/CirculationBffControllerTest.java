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
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.folio.circulationbff.domain.dto.BffSearchInstance;
import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.folio.circulationbff.domain.dto.StaffSlip;
import org.folio.circulationbff.domain.dto.StaffSlipsCollection;
import org.folio.circulationbff.domain.dto.User;
import org.folio.circulationbff.domain.dto.UserCollection;
import org.folio.circulationbff.service.CirculationBffService;
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
    StaffSlipsCollection staffSlipsCollection = new StaffSlipsCollection(
      Objects.isNull(staffSlips) ? 0 : staffSlips.size(), staffSlips);

    when(circulationBffService.fetchPickSlipsByServicePointId(anyString()))
      .thenReturn(staffSlipsCollection);

    ResponseEntity<StaffSlipsCollection> actual = controller.getPickSlips(StringUtils.EMPTY);

    assertThat(actual.getStatusCode(), is(HttpStatus.OK));
    assertThat(actual.getBody(), is(staffSlipsCollection));
  }

  @ParameterizedTest
  @MethodSource("staffSlips")
  void getSearchSlipsControllerReturnsSuccessResponseEntity(List<StaffSlip> staffSlips) {
    StaffSlipsCollection staffSlipsCollection = new StaffSlipsCollection(
      Objects.isNull(staffSlips) ? 0 : staffSlips.size(), staffSlips);

    when(circulationBffService.fetchSearchSlipsByServicePointId(anyString()))
      .thenReturn(staffSlipsCollection);

    ResponseEntity<StaffSlipsCollection> actual = controller.getSearchSlips(StringUtils.EMPTY);

    assertThat(actual.getStatusCode(), is(HttpStatus.OK));
    assertThat(actual.getBody(), is(staffSlipsCollection));
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
}
