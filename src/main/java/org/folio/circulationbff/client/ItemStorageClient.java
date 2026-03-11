package org.folio.circulationbff.client;

import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.Items;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "item-storage/items", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface ItemStorageClient extends GetByQueryParamsClient<Items> {

  @GetExchange("/{id}")
  Item findItem(@PathVariable String id);

}
