package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.SearchInstances;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "search", url = "search/instances", configuration = FeignClientConfiguration.class)
public interface SearchInstancesClient extends GetByQueryParamsClient<SearchInstances> {

  @GetMapping
  SearchInstances findInstances(@RequestParam String query, @RequestParam boolean expandAll);

}
