package org.folio.circulationbff.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.circulationbff.client.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.BatchRequest;
import org.folio.circulationbff.domain.dto.BatchRequestCollectionResponse;
import org.folio.circulationbff.domain.dto.BatchRequestDetail;
import org.folio.circulationbff.domain.dto.BatchRequestDetailsResponse;
import org.folio.circulationbff.domain.dto.BatchRequestResponse;
import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.folio.circulationbff.domain.dto.MediatedRequests;
import org.folio.circulationbff.service.impl.MediatedBatchRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class MediatedBatchRequestServiceTest {
  @Mock
  private RequestMediatedClient requestMediatedClient;
  @Mock
  private TenantService tenantService;

  @InjectMocks
  private MediatedBatchRequestServiceImpl service;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "batchRequestDetailsQueryIdsSize", 50);
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

  // cases for secure tenant

  @Test
  void retrieveBatchRequestDetailsUpdatesConfirmedRequestIdForSecureTenant() {
    var batchId = UUID.randomUUID();
    var offset = 0;
    var limit = 5;
    var mediatedRequestId = "mediated-request-id";
    var circulationRequestId = "circulation-request-id";

    var detail = new BatchRequestDetail().confirmedRequestId(mediatedRequestId);
    var batchDetails = new BatchRequestDetailsResponse().mediatedBatchRequestDetails(List.of(detail));

    var mediatedRequest = new MediatedRequest()
      .id(mediatedRequestId)
      .confirmedRequestId(circulationRequestId);
    var mediatedRequests = new MediatedRequests().mediatedRequests(List.of(mediatedRequest));

    when(requestMediatedClient.getMediatedBatchRequestDetails(batchId, limit, offset)).thenReturn(batchDetails);
    when(tenantService.isCurrentTenantSecure()).thenReturn(true);
    when(requestMediatedClient.getMediatedRequestsByQuery(anyString())).thenReturn(mediatedRequests);

    var response = service.retrieveMediatedBatchRequestDetails(batchId, offset, limit);

    assertThat(response.getMediatedBatchRequestDetails().getFirst().getConfirmedRequestId(), is(circulationRequestId));
  }

  @Test
  void retrieveBatchRequestDetailsWhenMediatedRequestNotFoundForSecureTenant() {
    var batchId = UUID.randomUUID();
    var offset = 0;
    var limit = 5;
    var mediatedRequestId = "mediated-request-id";

    var detail = new BatchRequestDetail().confirmedRequestId(mediatedRequestId);
    var batchDetails = new BatchRequestDetailsResponse().mediatedBatchRequestDetails(List.of(detail));
    var mediatedRequests = new MediatedRequests().mediatedRequests(List.of());

    when(requestMediatedClient.getMediatedBatchRequestDetails(batchId, limit, offset)).thenReturn(batchDetails);
    when(tenantService.isCurrentTenantSecure()).thenReturn(true);
    when(requestMediatedClient.getMediatedRequestsByQuery(anyString())).thenReturn(mediatedRequests);

    var response = service.retrieveMediatedBatchRequestDetails(batchId, offset, limit);

    assertThat(response.getMediatedBatchRequestDetails().getFirst().getConfirmedRequestId(), is(nullValue()));
  }

  @Test
  void retrieveBatchRequestDetailsHandlesMixedConfirmedRequestIdsForSecureTenant() {
    var batchId = UUID.randomUUID();
    var offset = 0;
    var limit = 5;
    var mediatedRequestId = UUID.randomUUID().toString();
    var circulationRequestId = UUID.randomUUID().toString();

    var detailWithNull = new BatchRequestDetail().confirmedRequestId(null);
    var detailWithId = new BatchRequestDetail().confirmedRequestId(mediatedRequestId);
    var batchDetails = new BatchRequestDetailsResponse()
      .mediatedBatchRequestDetails(List.of(detailWithNull, detailWithId));

    var mediatedRequest = new MediatedRequest()
      .id(mediatedRequestId)
      .confirmedRequestId(circulationRequestId);
    var mediatedRequests = new MediatedRequests().mediatedRequests(List.of(mediatedRequest));

    when(requestMediatedClient.getMediatedBatchRequestDetails(batchId, limit, offset)).thenReturn(batchDetails);
    when(tenantService.isCurrentTenantSecure()).thenReturn(true);
    when(requestMediatedClient.getMediatedRequestsByQuery(anyString())).thenReturn(mediatedRequests);

    var response = service.retrieveMediatedBatchRequestDetails(batchId, offset, limit);

    assertThat(response.getMediatedBatchRequestDetails().get(0).getConfirmedRequestId(), is(nullValue()));
    assertThat(response.getMediatedBatchRequestDetails().get(1).getConfirmedRequestId(), is(circulationRequestId));
  }

  @Test
  void retrieveBatchRequestDetailsWhenMediatedRequestHasBlankConfirmedRequestId() {
    var batchId = UUID.randomUUID();
    var offset = 0;
    var limit = 5;
    var mediatedRequestId = UUID.randomUUID().toString();

    var detail = new BatchRequestDetail().confirmedRequestId(mediatedRequestId);
    var batchDetails = new BatchRequestDetailsResponse().mediatedBatchRequestDetails(List.of(detail));

    var mediatedRequest = new MediatedRequest()
      .id(mediatedRequestId)
      .confirmedRequestId("");
    var mediatedRequests = new MediatedRequests().mediatedRequests(List.of(mediatedRequest));

    when(requestMediatedClient.getMediatedBatchRequestDetails(batchId, limit, offset)).thenReturn(batchDetails);
    when(tenantService.isCurrentTenantSecure()).thenReturn(true);
    when(requestMediatedClient.getMediatedRequestsByQuery(anyString())).thenReturn(mediatedRequests);

    var response = service.retrieveMediatedBatchRequestDetails(batchId, offset, limit);

    assertThat(response.getMediatedBatchRequestDetails().getFirst().getConfirmedRequestId(), is(nullValue()));
  }
}
