package org.folio.circulationbff.client.feign;

import java.util.Map;

import org.folio.circulationbff.domain.dto.SearchInstances;
import org.folio.circulationbff.support.CqlQuery;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "search/instances", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface SearchInstancesClient extends GetByQueryParamsClient<SearchInstances> {

  @Override
  @GetExchange
  SearchInstances getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @Override
  @GetExchange
  SearchInstances getByQueryParams(@RequestParam Map<String, String> queryParams);

  @GetExchange
  SearchInstances findInstances(@RequestParam String query, @RequestParam boolean expandAll);

}
