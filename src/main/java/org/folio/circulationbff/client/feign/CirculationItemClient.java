package org.folio.circulationbff.client.feign;

import java.util.Optional;

import org.folio.circulationbff.domain.dto.CirculationItem;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange(url = "circulation-item", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface CirculationItemClient {

  @GetExchange("/{circulationItemId}")
  Optional<CirculationItem> getCirculationItem(@PathVariable String circulationItemId);

  @PostExchange("/{circulationItemId}")
  Optional<CirculationItem> createCirculationItem(@PathVariable String circulationItemId,
    @RequestBody CirculationItem circulationItem);

  @PutExchange("/{circulationItemId}")
  Optional<CirculationItem> updateCirculationItem(@PathVariable String circulationItemId,
    @RequestBody CirculationItem circulationItem);

}
