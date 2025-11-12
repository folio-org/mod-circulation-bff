package org.folio.circulationbff.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.folio.circulationbff.client.feign.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.BatchRequest;
import org.folio.circulationbff.domain.dto.BatchRequestCollectionResponse;
import org.folio.circulationbff.domain.dto.BatchRequestDetailsResponse;
import org.folio.circulationbff.domain.dto.BatchRequestResponse;
import org.folio.circulationbff.service.impl.MediatedBatchRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class MediatedBatchRequestServiceTest {
  private RequestMediatedClient requestMediatedClient;
  private MediatedBatchRequestServiceImpl service;

  @BeforeEach
  void setUp() {
    requestMediatedClient = mock(RequestMediatedClient.class);
    service = new MediatedBatchRequestServiceImpl(requestMediatedClient);
  }

  @Test
  void createMediatedBatchRequestDelegatesToClient() {
    var batchRequest = new BatchRequest();
    var batchResponse = new BatchRequestResponse();
    var expectedResponse = ResponseEntity.ok(batchResponse);

    when(requestMediatedClient.postMediatedBatchRequest(batchRequest)).thenReturn(expectedResponse);

    var response = service.createMediatedBatchRequest(batchRequest);

    assertThat(response, is(expectedResponse));
    verify(requestMediatedClient).postMediatedBatchRequest(batchRequest);
  }

  @Test
  void retrieveMediatedBatchRequestByIdDelegatesToClient() {
    var batchId = UUID.randomUUID();
    var batchResponse = new BatchRequestResponse();
    var expectedResponse = ResponseEntity.ok(batchResponse);

    when(requestMediatedClient.getMediatedBatchRequestById(batchId)).thenReturn(expectedResponse);

    var response = service.retrieveMediatedBatchRequestById(batchId);

    assertThat(response, is(expectedResponse));
    verify(requestMediatedClient).getMediatedBatchRequestById(batchId);
  }

  @Test
  void retrieveMediatedBatchRequestsByQueryDelegatesToClient() {
    var query = "status==OPEN";
    var offset = 0;
    var limit = 10;
    var expectedCollection = new BatchRequestCollectionResponse();

    when(requestMediatedClient.getMediatedBatchRequestsByQuery(query, limit, offset)).thenReturn(expectedCollection);

    var response = service.retrieveMediatedBatchRequestsByQuery(query, offset, limit);

    assertThat(response, is(expectedCollection));
    verify(requestMediatedClient).getMediatedBatchRequestsByQuery(query, limit, offset);
  }

  @Test
  void retrieveMediatedBatchRequestDetailsDelegatesToClient() {
    var batchId = UUID.randomUUID();
    var offset = 0;
    var limit = 5;
    var expectedDetails = new BatchRequestDetailsResponse();

    when(requestMediatedClient.getMediatedBatchRequestDetails(batchId, limit, offset)).thenReturn(expectedDetails);

    var response = service.retrieveMediatedBatchRequestDetails(batchId, offset, limit);

    assertThat(response, is(expectedDetails));
    verify(requestMediatedClient).getMediatedBatchRequestDetails(batchId, limit, offset);
  }

}
