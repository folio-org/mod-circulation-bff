package org.folio.circulationbff.client;

import org.folio.circulationbff.domain.dto.InventoryItem;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "inventory", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface InventoryClient {

  @GetExchange("/items/{id}")
  InventoryItem getById(@PathVariable String id);

}
