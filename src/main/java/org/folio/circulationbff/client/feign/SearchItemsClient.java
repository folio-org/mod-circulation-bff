package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.ConsortiumItem;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "search-items", url = "search", configuration = FeignClientConfiguration.class)
public interface SearchItemsClient {

  @GetMapping("/consortium/item/{itemId}")
  ConsortiumItem searchItem(@PathVariable("itemId") String itemId);
}
