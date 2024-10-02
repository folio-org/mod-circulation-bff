package org.folio.circulationbff.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.Instance;
import org.folio.circulationbff.domain.dto.InstanceSearchResult;
import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.folio.circulationbff.service.MediatedRequestsService;
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
    String query = "id=="  + UUID.randomUUID();
    InstanceSearchResult mockSearchResponse = new InstanceSearchResult()
      .addInstancesItem(new Instance().id(UUID.randomUUID().toString()))
      .totalRecords(1);
    when(searchService.findInstances(query))
      .thenReturn(mockSearchResponse);
    var response = controller.circulationBffRequestsSearchInstancesGet(query);
    assertThat(response.getStatusCode(), is(HttpStatus.OK));
    assertThat(response.getBody(), equalTo(mockSearchResponse));
  }

  @Test
  void saveAndConfirmMediatedRequestShouldReturnCreatedStatus() {
    MediatedRequest mediatedRequest = new MediatedRequest();
    mediatedRequest.setId(UUID.randomUUID().toString());
    ResponseEntity<Void> mockResponseEntity = ResponseEntity.noContent().build();
    when(mediatedRequestsService.updateAndConfirmMediatedRequest(mediatedRequest)).thenReturn(mockResponseEntity);
    ResponseEntity<MediatedRequest> response = controller.saveAndConfirmMediatedRequest(mediatedRequest);

    assertThat(response.getStatusCode(), is(HttpStatus.CREATED));
    assertThat(response.getBody(), is(mediatedRequest));
  }

  @Test
  void saveAndConfirmMediatedRequestShouldReturnBadRequest() {
    MediatedRequest mediatedRequest = new MediatedRequest();
    mediatedRequest.setId(UUID.randomUUID().toString());
    ResponseEntity<Void> mockResponseEntity = ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

    when(mediatedRequestsService.updateAndConfirmMediatedRequest(mediatedRequest)).thenReturn(mockResponseEntity);
    ResponseEntity<MediatedRequest> response = controller.saveAndConfirmMediatedRequest(mediatedRequest);

    assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
    assertNull(response.getBody());
  }
}
