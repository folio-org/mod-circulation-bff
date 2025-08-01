package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.Campus;
import org.folio.circulationbff.domain.dto.Campuses;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "campuses", url = "location-units/campuses", configuration = FeignClientConfiguration.class)
public interface LocationCampusClient extends GetByQueryParamsClient<Campuses> {

  @GetMapping("/{id}")
  Campus findCampus(@PathVariable String id);
}
