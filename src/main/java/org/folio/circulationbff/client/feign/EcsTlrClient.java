package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.AllowedServicePointParams;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.BffRequest;
import org.folio.circulationbff.domain.dto.Request;
import org.folio.circulationbff.domain.dto.TlrSettings;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ecs-tlr", url = "tlr", configuration = FeignClientConfiguration.class)
public interface EcsTlrClient {

  @GetMapping("/allowed-service-points")
  AllowedServicePoints getAllowedServicePoints(@SpringQueryMap AllowedServicePointParams params);

  @GetMapping("/settings")
  TlrSettings getTlrSettings();

  @PostMapping("/ecs-tlr")
  Request createRequest(@RequestBody BffRequest request);
}
