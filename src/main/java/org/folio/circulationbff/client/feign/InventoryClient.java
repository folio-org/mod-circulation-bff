package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.client.feign.config.ErrorForwardingFeignClientConfiguration;
import org.folio.circulationbff.domain.dto.InventoryItem;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "inventory", url = "inventory",
  configuration = { FeignClientConfiguration.class, ErrorForwardingFeignClientConfiguration.class })
public interface InventoryClient {

  @GetMapping("items/{id}")
  InventoryItem getById(@PathVariable String id);

}
