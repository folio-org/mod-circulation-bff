package org.folio.circulationbff.service;

import java.util.UUID;
import org.folio.circulationbff.domain.dto.BatchRequest;
import org.folio.circulationbff.domain.dto.BatchRequestCollectionResponse;
import org.folio.circulationbff.domain.dto.BatchRequestDetailsResponse;
import org.folio.circulationbff.domain.dto.BatchRequestResponse;
import org.springframework.http.ResponseEntity;

public interface MediatedBatchRequestService {
  ResponseEntity<BatchRequestResponse> createMediatedBatchRequest(BatchRequest batchRequest);
  ResponseEntity<BatchRequestResponse> retrieveMediatedBatchRequestById(UUID batchRequestId);
  BatchRequestCollectionResponse retrieveMediatedBatchRequestsByQuery(String query, Integer offset, Integer limit);
  BatchRequestDetailsResponse retrieveMediatedBatchRequestDetails(UUID batchRequestId, Integer offset, Integer limit);
}
