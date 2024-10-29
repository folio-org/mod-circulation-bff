package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.Instance;
import org.folio.circulationbff.domain.dto.Instances;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "instance-storage", url = "instance-storage/instances",
  configuration = FeignClientConfiguration.class)
public interface InstanceStorageClient extends GetByQueryClient<Instances> {

  @GetMapping("/{id}")
  Instance findInstance(@PathVariable String id);
}
