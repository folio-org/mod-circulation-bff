package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.Institution;
import org.folio.circulationbff.domain.dto.Institutions;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "institutions", url = "location-units/institutions", configuration = FeignClientConfiguration.class)
public interface LocationInstitutionClient extends GetByQueryParamsClient<Institutions> {

  @GetMapping("/{id}")
  Institution findInstitution(@PathVariable String id);
}
