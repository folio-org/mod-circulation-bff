package org.folio.circulationbff.client;

import java.util.Map;

import org.folio.circulationbff.support.CqlQuery;
import org.springframework.web.bind.annotation.RequestParam;

public interface GetByQueryParamsClient<T> {
  T getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);
  T getByQueryParams(@RequestParam Map<String, String> queryParams);
}
