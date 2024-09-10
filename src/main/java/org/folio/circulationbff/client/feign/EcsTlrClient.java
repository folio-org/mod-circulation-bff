package org.folio.circulationbff.client.feign;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.TlrSettings;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ecs-tlr", url = "/tlr", configuration = FeignClientConfiguration.class)
public interface EcsTlrClient {

  @GetMapping("/allowed-service-points")
  AllowedServicePoints getAllowedServicePoints(
    @RequestParam("operation") String operation, @RequestParam("requesterId") UUID requesterId,
    @RequestParam("instanceId") UUID instanceId, @RequestParam("requestId") UUID requestId);

  @GetMapping("/settings")
  TlrSettings getTlrSettings ();

}
