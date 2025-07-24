package org.folio.circulationbff.client.feign;

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
import org.folio.circulationbff.domain.dto.TlrDeclareItemLostRequest;
import org.folio.circulationbff.domain.dto.TlrSettings;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ecs-tlr", url = "tlr", configuration = FeignClientConfiguration.class)
public interface EcsTlrClient {

  @GetMapping("/allowed-service-points")
  AllowedServicePoints getAllowedServicePoints(@SpringQueryMap AllowedServicePointParams params);

  @GetMapping("/settings")
  TlrSettings getTlrSettings();

  @PostMapping("/ecs-tlr")
  EcsTlr createRequest(@RequestBody BffRequest request);

  @GetMapping("/staff-slips/pick-slips/{servicePointId}")
  PickSlipCollection getPickSlips(@PathVariable("servicePointId") String servicePointId);

  @GetMapping("/staff-slips/search-slips/{servicePointId}")
  SearchSlipCollection getSearchSlips(@PathVariable ("servicePointId") String servicePointId);

  @PostMapping("/create-ecs-request-external")
  EcsTlr createEcsExternalRequest(@RequestBody EcsRequestExternal request);

  @PostMapping("/loans/check-out-by-barcode")
  CheckOutResponse checkOutByBarcode(@RequestBody CheckOutRequest checkOutRequest);

  @PostMapping("/loans/declare-item-lost")
  ResponseEntity<Void> declareItemLost(@RequestBody TlrDeclareItemLostRequest request);

  @PostMapping("/loans/claim-item-returned")
  ResponseEntity<Void> claimItemReturned(@RequestBody TlrClaimItemReturnedRequest request);
}
