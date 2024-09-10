package org.folio.circulationbff.client.feign;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "circulation", url = "circulation", configuration = FeignClientConfiguration.class)
public interface CirculationClient {

  @GetMapping("/requests/allowed-service-points")
  AllowedServicePoints allowedServicePoints (
    @RequestParam("patronGroupId") UUID patronGroupId, @RequestParam("instanceId") UUID instanceId,
    @RequestParam("operation") String operation, @RequestParam("requestId") UUID requestId);



}
