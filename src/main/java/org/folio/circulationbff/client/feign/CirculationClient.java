package org.folio.circulationbff.client.feign;

import java.util.UUID;

import org.folio.circulationbff.client.feign.config.ErrorForwardingFeignClientConfiguration;
import org.folio.circulationbff.domain.dto.AllowedServicePointParams;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.BffRequest;
import org.folio.circulationbff.domain.dto.CirculationLoan;
import org.folio.circulationbff.domain.dto.CirculationLoans;
import org.folio.circulationbff.domain.dto.CirculationSettingsResponse;
import org.folio.circulationbff.domain.dto.DeclareItemLostRequest;
import org.folio.circulationbff.domain.dto.PickSlipCollection;
import org.folio.circulationbff.domain.dto.Request;
import org.folio.circulationbff.domain.dto.SearchSlipCollection;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "circulation", url = "circulation",
  configuration = { FeignClientConfiguration.class, ErrorForwardingFeignClientConfiguration.class })
public interface CirculationClient {

  @GetMapping("/requests/allowed-service-points")
  AllowedServicePoints allowedServicePoints (@SpringQueryMap AllowedServicePointParams params);

  @GetMapping(value = "/settings")
  CirculationSettingsResponse getCirculationSettingsByQuery(@RequestParam("query") String query);

  @PostMapping("/requests")
  Request createRequest(@RequestBody BffRequest request);

  @GetMapping("/requests/{requestId}")
  Request getRequestById(@PathVariable("requestId") String requestId);

  @GetMapping("/pick-slips/{servicePointId}")
  PickSlipCollection getPickSlips(@PathVariable ("servicePointId") String servicePointId);

  @GetMapping("/search-slips/{servicePointId}")
  SearchSlipCollection getSearchSlips(@PathVariable ("servicePointId") String servicePointId);

  @PostMapping("/loans/{loanId}/declare-item-lost")
  ResponseEntity<Void> declareItemLost(@PathVariable("loanId") UUID loanId,
    @RequestBody DeclareItemLostRequest request);

  @GetMapping("/loans/{id}")
  CirculationLoan findLoanById(@PathVariable("id") UUID id);

  @GetMapping("/loans")
  CirculationLoans findLoansByQuery(
    @RequestParam("query") String query,
    @RequestParam("limit") Integer limit,
    @RequestParam("offset") Integer offset,
    @RequestParam("totalRecords") String totalRecords);
}
