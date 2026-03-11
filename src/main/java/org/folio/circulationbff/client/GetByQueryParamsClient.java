package org.folio.circulationbff.client;

import java.util.Map;

import org.folio.circulationbff.support.CqlQuery;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(contentType = MediaType.APPLICATION_JSON_VALUE, accept = MediaType.APPLICATION_JSON_VALUE)
public interface GetByQueryParamsClient<T> {

  @GetExchange
  T getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @GetExchange
  T getByQueryParams(@RequestParam Map<String, String> queryParams);
}
