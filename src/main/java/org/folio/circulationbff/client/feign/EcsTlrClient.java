package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.*;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
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
}
