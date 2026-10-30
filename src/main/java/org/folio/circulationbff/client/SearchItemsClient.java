package org.folio.circulationbff.client;

import org.folio.circulationbff.domain.dto.ConsortiumItem;
import org.folio.circulationbff.domain.dto.ConsortiumItemBatchRequest;
import org.folio.circulationbff.domain.dto.ConsortiumItemCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "search", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface SearchItemsClient {

  @GetExchange("/consortium/item/{itemId}")
  ConsortiumItem searchItem(@PathVariable("itemId") String itemId);

  @PostExchange("/consortium/batch/items")
  ConsortiumItemCollection searchItemsBatch(@RequestBody ConsortiumItemBatchRequest request);
}
