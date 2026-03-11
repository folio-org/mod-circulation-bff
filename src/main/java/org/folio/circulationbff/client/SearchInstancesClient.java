package org.folio.circulationbff.client;

import org.folio.circulationbff.domain.dto.SearchInstances;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "search/instances", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface SearchInstancesClient extends GetByQueryParamsClient<SearchInstances> {

  @GetExchange()
  SearchInstances findInstances(@RequestParam String query, @RequestParam boolean expandAll);

}

