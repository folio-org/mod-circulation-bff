package org.folio.circulationbff.client;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.AllowedServicePointParams;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.BffRequest;
import org.folio.circulationbff.domain.dto.CheckOutRequest;
import org.folio.circulationbff.domain.dto.CheckOutResponse;
import org.folio.circulationbff.domain.dto.EcsRequestExternal;
import org.folio.circulationbff.domain.dto.EcsTlr;
import org.folio.circulationbff.domain.dto.PickSlipCollection;
import org.folio.circulationbff.domain.dto.SearchSlipCollection;
import org.folio.circulationbff.domain.dto.TlrClaimItemReturnedRequest;
import org.folio.circulationbff.domain.dto.TlrDeclareClaimedReturnedItemAsMissingRequest;
import org.folio.circulationbff.domain.dto.TlrDeclareItemLostRequest;
import org.folio.circulationbff.domain.dto.TlrSettings;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "tlr", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface EcsTlrClient {

  default AllowedServicePoints getAllowedServicePoints(AllowedServicePointParams params) {
    return getAllowedServicePoints(params.getOperation(), params.getPatronGroupId(),
      params.getInstanceId(), params.getRequestId(), params.getRequesterId(), params.getItemId());
  }

  @GetExchange("/allowed-service-points")
  AllowedServicePoints getAllowedServicePoints(@RequestParam("operation") String operation,
    @RequestParam(value = "patronGroupId", required = false) UUID patronGroupId,
    @RequestParam(value = "instanceId", required = false) UUID instanceId,
    @RequestParam(value = "requestId", required = false) UUID requestId,
    @RequestParam(value = "requesterId", required = false) UUID requesterId,
    @RequestParam(value = "itemId", required = false) UUID itemId);

  @GetExchange("/settings")
  TlrSettings getTlrSettings();

  @PostExchange("/ecs-tlr")
  EcsTlr createRequest(@RequestBody BffRequest request);

  @GetExchange("/staff-slips/pick-slips/{servicePointId}")
  PickSlipCollection getPickSlips(@PathVariable("servicePointId") String servicePointId);

  @GetExchange("/staff-slips/search-slips/{servicePointId}")
  SearchSlipCollection getSearchSlips(@PathVariable ("servicePointId") String servicePointId);

  @PostExchange("/create-ecs-request-external")
  EcsTlr createEcsExternalRequest(@RequestBody EcsRequestExternal request);

  @PostExchange("/loans/check-out-by-barcode")
  CheckOutResponse checkOutByBarcode(@RequestBody CheckOutRequest checkOutRequest);

  @PostExchange("/loans/declare-item-lost")
  ResponseEntity<Void> declareItemLost(@RequestBody TlrDeclareItemLostRequest request);

  @PostExchange("/loans/claim-item-returned")
  ResponseEntity<Void> claimItemReturned(@RequestBody TlrClaimItemReturnedRequest request);

  @PostExchange("/loans/declare-claimed-returned-item-as-missing")
  ResponseEntity<Void> declareClaimedReturnedItemAsMissing(
    @RequestBody TlrDeclareClaimedReturnedItemAsMissingRequest request);
}
