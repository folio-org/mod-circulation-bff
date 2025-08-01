package org.folio.circulationbff.client.feign;

import java.util.Map;

import org.folio.circulationbff.support.CqlQuery;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name="get-by-query-params", configuration = FeignClientConfiguration.class)
public interface GetByQueryParamsClient<T> {

  @GetMapping
  T getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @GetMapping
  T getByQueryParams(@RequestParam Map<String, String> queryParams);
}
