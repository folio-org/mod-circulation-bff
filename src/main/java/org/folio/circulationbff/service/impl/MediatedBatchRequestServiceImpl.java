package org.folio.circulationbff.service.impl;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.circulationbff.client.feign.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.BatchRequest;
import org.folio.circulationbff.domain.dto.BatchRequestCollectionResponse;
import org.folio.circulationbff.domain.dto.BatchRequestDetailsResponse;
import org.folio.circulationbff.domain.dto.BatchRequestResponse;
import org.folio.circulationbff.service.MediatedBatchRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class MediatedBatchRequestServiceImpl implements MediatedBatchRequestService {

  private final RequestMediatedClient requestMediatedClient;

  @Override
  public ResponseEntity<BatchRequestResponse> createMediatedBatchRequest(BatchRequest batchRequest) {
    log.debug("createMediatedBatchRequest:: parameters batchRequest: {}", batchRequest);

    return requestMediatedClient.postMediatedBatchRequest(batchRequest);
  }

  @Override
  public ResponseEntity<BatchRequestResponse> retrieveMediatedBatchRequestById(UUID batchRequestId) {
    return requestMediatedClient.getMediatedBatchRequestById(batchRequestId);
  }

  @Override
  public BatchRequestCollectionResponse retrieveMediatedBatchRequestsByQuery(String query, Integer offset, Integer limit) {
    return requestMediatedClient.getMediatedBatchRequestsByQuery(query, limit, offset);
  }

  @Override
  public BatchRequestDetailsResponse retrieveMediatedBatchRequestDetails(UUID batchRequestId, Integer offset, Integer limit) {
    return requestMediatedClient.getMediatedBatchRequestDetails(batchRequestId, limit, offset);
  }
}
