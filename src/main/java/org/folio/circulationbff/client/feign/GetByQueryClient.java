package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.support.CqlQuery;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="get-by-query", configuration = FeignClientConfiguration.class)
public interface GetByQueryClient<T> {

  @GetMapping
  T getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);
}
