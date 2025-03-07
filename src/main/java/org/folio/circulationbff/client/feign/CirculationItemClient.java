package org.folio.circulationbff.client.feign;

import java.util.Optional;

import org.folio.circulationbff.domain.dto.CirculationItem;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "circulation-item", url = "circulation-item",
  configuration = FeignClientConfiguration.class, dismiss404 = true)
public interface CirculationItemClient {

  @GetMapping(value = "/{circulationItemId}")
  Optional<CirculationItem> getCirculationItem(@PathVariable String circulationItemId);

  @PostMapping(value = "/{circulationItemId}")
  Optional<CirculationItem> createCirculationItem(@PathVariable String circulationItemId,
    @RequestBody CirculationItem circulationItem);

  @PutMapping(value = "/{circulationItemId}")
  Optional<CirculationItem> updateCirculationItem(@PathVariable String circulationItemId,
    @RequestBody CirculationItem circulationItem);

}
