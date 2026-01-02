package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.ServicePoint;
import org.folio.circulationbff.domain.dto.ServicePoints;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "service-points", url = "service-points",
  configuration = FeignClientConfiguration.class)
public interface ServicePointClient extends GetByQueryParamsClient<ServicePoints> {

  @GetMapping("/{id}")
  ServicePoint findServicePoint(@PathVariable String id);
}
