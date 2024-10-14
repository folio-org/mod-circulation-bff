package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.HoldingsRecord;
import org.folio.circulationbff.domain.dto.HoldingsRecords;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "holdings-storage", url = "holdings-storage/holdings", configuration = FeignClientConfiguration.class)
public interface HoldingsStorageClient extends GetByQueryClient<HoldingsRecords> {

  @GetMapping("/{id}")
  HoldingsRecord findHoldingsRecord(@PathVariable String id);
}
