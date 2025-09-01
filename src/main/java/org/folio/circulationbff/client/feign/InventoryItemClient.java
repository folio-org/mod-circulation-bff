package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.InventoryItem;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "inventory-item", url = "inventory/items",
  configuration = FeignClientConfiguration.class)
public interface InventoryItemClient extends GetByQueryParamsClient<InventoryItem> {

  @GetMapping("/{id}")
  InventoryItem getById(@PathVariable String id);

}
