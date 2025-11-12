package org.folio.circulationbff.client.feign;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.BatchRequest;
import org.folio.circulationbff.domain.dto.BatchRequestCollectionResponse;
import org.folio.circulationbff.domain.dto.BatchRequestDetailsResponse;
import org.folio.circulationbff.domain.dto.BatchRequestResponse;
import org.folio.circulationbff.domain.dto.CheckOutRequest;
import org.folio.circulationbff.domain.dto.CheckOutResponse;
import org.folio.circulationbff.domain.dto.ClaimItemReturnedRequest;
import org.folio.circulationbff.domain.dto.DeclareClaimedReturnedItemAsMissingRequest;
import org.folio.circulationbff.domain.dto.DeclareItemLostRequest;
import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "requests-mediated", url = "requests-mediated",
  configuration = FeignClientConfiguration.class)
public interface RequestMediatedClient {

  @PostMapping("/mediated-requests")
  ResponseEntity<MediatedRequest> postRequestMediated(@RequestBody MediatedRequest mediatedRequest);

  @PutMapping("/mediated-requests/{requestId}")
  ResponseEntity<Void> putRequestMediated(@PathVariable String requestId,
    @RequestBody MediatedRequest mediatedRequest);

  @PostMapping("/mediated-requests/{requestId}/confirm")
  ResponseEntity<Void> confirmRequestMediated(@PathVariable String requestId,
    @RequestBody MediatedRequest mediatedRequest);

  @PostMapping("/loans/check-out-by-barcode")
  CheckOutResponse checkOutByBarcode(@RequestBody CheckOutRequest request);

  @PostMapping("/loans/{loanId}/declare-item-lost")
  ResponseEntity<Void> declareItemLost(@PathVariable("loanId") UUID loanId,
    @RequestBody DeclareItemLostRequest request);

  @PostMapping("/loans/{loanId}/claim-item-returned")
  ResponseEntity<Void> claimItemReturned(@PathVariable("loanId") UUID loanId,
    @RequestBody ClaimItemReturnedRequest request);

  @PostMapping("/loans/{loanId}/declare-claimed-returned-item-as-missing")
  ResponseEntity<Void> declareClaimedReturnedItemAsMissing(@PathVariable("loanId") UUID loanId,
    @RequestBody DeclareClaimedReturnedItemAsMissingRequest request);

  @PostMapping("/batch-mediated-requests")
  ResponseEntity<BatchRequestResponse> postMediatedBatchRequest(@RequestBody BatchRequest batchRequest);

  @GetMapping("/batch-mediated-requests/{batchRequestId}")
  ResponseEntity<BatchRequestResponse> getMediatedBatchRequestById(@PathVariable("batchRequestId") UUID batchRequestId);

  @GetMapping("/batch-mediated-requests")
  BatchRequestCollectionResponse getMediatedBatchRequestsByQuery(
    @RequestParam("query") String query,
    @RequestParam("limit") Integer limit,
    @RequestParam("offset") Integer offset);

  @GetMapping("/batch-mediated-requests/{batchRequestId}/details")
  BatchRequestDetailsResponse getMediatedBatchRequestDetails(
    @PathVariable("batchRequestId") UUID batchRequestId,
    @RequestParam("limit") Integer limit,
    @RequestParam("offset") Integer offset);
}
