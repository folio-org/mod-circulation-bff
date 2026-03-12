package org.folio.circulationbff.client.feign;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.BatchRequest;
import org.folio.circulationbff.domain.dto.BatchRequestCollectionResponse;
import org.folio.circulationbff.domain.dto.BatchRequestDetailsResponse;
import org.folio.circulationbff.domain.dto.BatchRequestResponse;
import org.folio.circulationbff.domain.dto.CheckInRequest;
import org.folio.circulationbff.domain.dto.CheckInResponse;
import org.folio.circulationbff.domain.dto.CheckOutRequest;
import org.folio.circulationbff.domain.dto.CheckOutResponse;
import org.folio.circulationbff.domain.dto.ClaimItemReturnedRequest;
import org.folio.circulationbff.domain.dto.DeclareClaimedReturnedItemAsMissingRequest;
import org.folio.circulationbff.domain.dto.DeclareItemLostRequest;
import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.folio.circulationbff.domain.dto.MediatedRequests;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange(url = "requests-mediated", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface RequestMediatedClient {

  @PostExchange("/mediated-requests")
  ResponseEntity<MediatedRequest> postRequestMediated(@RequestBody MediatedRequest mediatedRequest);

  @PutExchange("/mediated-requests/{requestId}")
  ResponseEntity<Void> putRequestMediated(@PathVariable String requestId,
    @RequestBody MediatedRequest mediatedRequest);

  @PostExchange("/mediated-requests/{requestId}/confirm")
  ResponseEntity<Void> confirmRequestMediated(@PathVariable String requestId,
    @RequestBody MediatedRequest mediatedRequest);

  @GetExchange("/mediated-requests")
  MediatedRequests getMediatedRequestsByQuery(@RequestParam("query") String query);

  @PostExchange("/loans/check-out-by-barcode")
  CheckOutResponse checkOutByBarcode(@RequestBody CheckOutRequest request);

  @PostExchange("/loans/check-in-by-barcode")
  CheckInResponse checkInByBarcode(@RequestBody CheckInRequest request);

  @PostExchange("/loans/{loanId}/declare-item-lost")
  ResponseEntity<Void> declareItemLost(@PathVariable("loanId") UUID loanId,
    @RequestBody DeclareItemLostRequest request);

  @PostExchange("/loans/{loanId}/claim-item-returned")
  ResponseEntity<Void> claimItemReturned(@PathVariable("loanId") UUID loanId,
    @RequestBody ClaimItemReturnedRequest request);

  @PostExchange("/loans/{loanId}/declare-claimed-returned-item-as-missing")
  ResponseEntity<Void> declareClaimedReturnedItemAsMissing(@PathVariable("loanId") UUID loanId,
    @RequestBody DeclareClaimedReturnedItemAsMissingRequest request);

  @PostExchange("/batch-mediated-requests")
  ResponseEntity<BatchRequestResponse> postMediatedBatchRequest(@RequestBody BatchRequest batchRequest);

  @GetExchange("/batch-mediated-requests/{batchRequestId}")
  ResponseEntity<BatchRequestResponse> getMediatedBatchRequestById(@PathVariable("batchRequestId") UUID batchRequestId);

  @GetExchange("/batch-mediated-requests")
  BatchRequestCollectionResponse getMediatedBatchRequestsByQuery(
    @RequestParam("query") String query,
    @RequestParam("limit") Integer limit,
    @RequestParam("offset") Integer offset);

  @GetExchange("/batch-mediated-requests/{batchRequestId}/details")
  BatchRequestDetailsResponse getMediatedBatchRequestDetails(
    @PathVariable("batchRequestId") UUID batchRequestId,
    @RequestParam("limit") Integer limit,
    @RequestParam("offset") Integer offset);

  @GetExchange("/batch-mediated-requests/details")
  BatchRequestDetailsResponse queryMediatedBatchRequestDetails(@RequestParam("query") String query);
}
