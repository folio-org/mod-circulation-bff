package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.AllowedServicePointParams;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.BffRequest;
import org.folio.circulationbff.domain.dto.CirculationSettingsResponse;
import org.folio.circulationbff.domain.dto.Request;
import org.folio.circulationbff.domain.dto.StaffSlipCollection;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "circulation", url = "circulation",
  configuration = FeignClientConfiguration.class)
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
  StaffSlipCollection getPickSlips(@PathVariable ("servicePointId") String servicePointId);

  @GetMapping("/search-slips/{servicePointId}")
  StaffSlipCollection getSearchSlips(@PathVariable ("servicePointId") String servicePointId);
}
