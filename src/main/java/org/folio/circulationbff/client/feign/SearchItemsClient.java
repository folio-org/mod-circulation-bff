package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.ConsortiumItem;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "search", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface SearchItemsClient {

  @GetExchange("/consortium/item/{itemId}")
  ConsortiumItem searchItem(@PathVariable("itemId") String itemId);
}
