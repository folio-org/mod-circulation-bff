package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.support.CqlQuery;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name="get-by-query", configuration = FeignClientConfiguration.class)
public interface GetByQueryClient<T> {

  @GetMapping("?query={query}")
  T getByQuery(@PathVariable CqlQuery query);
}
