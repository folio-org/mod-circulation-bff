package org.folio.circulationbff.client.feign;

import java.util.Map;

import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.Items;
import org.folio.circulationbff.support.CqlQuery;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "item-storage/items", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface ItemStorageClient extends GetByQueryParamsClient<Items> {

  @Override
  @GetExchange
  Items getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @Override
  @GetExchange
  Items getByQueryParams(@RequestParam Map<String, String> queryParams);

  @GetExchange("/{id}")
  Item findItem(@PathVariable String id);
}
