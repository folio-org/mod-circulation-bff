package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.Libraries;
import org.folio.circulationbff.domain.dto.Library;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "libraries", url = "location-units/libraries", configuration = FeignClientConfiguration.class)
public interface LocationLibraryClient extends GetByQueryParamsClient<Libraries> {

  @GetMapping("/{id}")
  Library findLibrary(@PathVariable String id);
}
