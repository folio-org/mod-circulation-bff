package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.MaterialType;
import org.folio.circulationbff.domain.dto.MaterialTypes;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "material-types", url = "material-types", configuration = FeignClientConfiguration.class)
public interface MaterialTypeClient extends GetByQueryClient<MaterialTypes> {

  @GetMapping("/{id}")
  MaterialType findMaterialType(@PathVariable String id);
}
