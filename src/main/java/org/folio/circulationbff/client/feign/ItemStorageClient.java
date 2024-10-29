package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.Items;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "item-storage", url = "item-storage/items",
  configuration = FeignClientConfiguration.class)
public interface ItemStorageClient extends GetByQueryClient<Items> {

  @GetMapping("/{id}")
  Item findItem(@PathVariable String id);
}
