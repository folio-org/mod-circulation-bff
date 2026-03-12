package org.folio.circulationbff.client;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.AllowedServicePointParams;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.BffRequest;
import org.folio.circulationbff.domain.dto.CirculationLoan;
import org.folio.circulationbff.domain.dto.CirculationLoans;
import org.folio.circulationbff.domain.dto.CirculationSettingsResponse;
import org.folio.circulationbff.domain.dto.DeclareClaimedReturnedItemAsMissingRequest;
import org.folio.circulationbff.domain.dto.DeclareItemLostRequest;
import org.folio.circulationbff.domain.dto.PickSlipCollection;
import org.folio.circulationbff.domain.dto.Request;
import org.folio.circulationbff.domain.dto.Requests;
import org.folio.circulationbff.domain.dto.SearchSlipCollection;
import org.folio.circulationbff.domain.dto.ClaimItemReturnedRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "circulation", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface CirculationClient {

  default AllowedServicePoints allowedServicePoints(AllowedServicePointParams params) {
    return allowedServicePoints(params.getOperation(), params.getPatronGroupId(), params.getInstanceId(),
      params.getRequestId(), params.getRequesterId(), params.getItemId());
  }

  @GetExchange("/requests/allowed-service-points")
  AllowedServicePoints allowedServicePoints(@RequestParam("operation") String operation,
    @RequestParam(value = "patronGroupId", required = false) UUID patronGroupId,
    @RequestParam(value = "instanceId", required = false) UUID instanceId,
    @RequestParam(value = "requestId", required = false) UUID requestId,
    @RequestParam(value = "requesterId", required = false) UUID requesterId,
    @RequestParam(value = "itemId", required = false) UUID itemId);

  @GetExchange("/settings")
  CirculationSettingsResponse getCirculationSettingsByQuery(@RequestParam("query") String query);

  @PostExchange("/requests")
  Request createRequest(@RequestBody BffRequest request);

  @GetExchange("/requests")
  Requests getRequests(
    @RequestParam("query") String query,
    @RequestParam("limit") Integer limit,
    @RequestParam("offset") Integer offset,
    @RequestParam("totalRecords") String totalRecords);

  @GetExchange("/requests/{requestId}")
  Request getRequestById(@PathVariable("requestId") String requestId);

  @GetExchange("/pick-slips/{servicePointId}")
  PickSlipCollection getPickSlips(@PathVariable ("servicePointId") String servicePointId);

  @GetExchange("/search-slips/{servicePointId}")
  SearchSlipCollection getSearchSlips(@PathVariable ("servicePointId") String servicePointId);

  @GetExchange("/loans/{id}")
  CirculationLoan findLoanById(@PathVariable("id") UUID id);

  @GetExchange("/loans")
  CirculationLoans findLoansByQuery(
    @RequestParam("query") String query,
    @RequestParam("limit") Integer limit,
    @RequestParam("offset") Integer offset,
    @RequestParam("totalRecords") String totalRecords);

  @PostExchange("/loans/{loanId}/declare-item-lost")
  ResponseEntity<Void> declareItemLost(@PathVariable("loanId") UUID loanId,
    @RequestBody DeclareItemLostRequest request);

  @PostExchange("/loans/{loanId}/claim-item-returned")
  ResponseEntity<Void> claimItemReturned(@PathVariable("loanId") UUID loanId,
    @RequestBody ClaimItemReturnedRequest request);

  @PostExchange("/loans/{loanId}/declare-claimed-returned-item-as-missing")
  ResponseEntity<Void> declareClaimedReturnedItemAsMissing(@PathVariable("loanId") UUID loanId,
    @RequestBody DeclareClaimedReturnedItemAsMissingRequest request);
}
