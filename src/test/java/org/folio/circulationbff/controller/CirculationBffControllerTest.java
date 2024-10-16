package org.folio.circulationbff.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.List;
import java.util.UUID;

import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.folio.circulationbff.service.MediatedRequestsService;
import org.folio.circulationbff.domain.dto.BffSearchInstance;
import org.folio.circulationbff.service.SearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

  @InjectMocks
  private CirculationBffController controller;

  @Test
  void instancesFoundSuccessfully() {
    String instanceId = UUID.randomUUID().toString();
    String query = "id=="  + instanceId;
    List<BffSearchInstance> mockSearchResponse = List.of(new BffSearchInstance().id(instanceId));
    when(searchService.findInstances(query))
      .thenReturn(mockSearchResponse);
    var response = controller.circulationBffRequestsSearchInstancesGet(query);
    assertThat(response.getStatusCode(), is(HttpStatus.OK));
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
