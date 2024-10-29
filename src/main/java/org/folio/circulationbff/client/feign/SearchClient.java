package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.SearchInstances;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "search", url = "search", configuration = FeignClientConfiguration.class)
public interface SearchClient {

  @GetMapping("/instances")
  SearchInstances findInstances(@RequestParam String query, @RequestParam boolean expandAll);
}
