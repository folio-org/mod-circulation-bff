package org.folio.circulationbff.client.feign;

import java.util.Map;

import org.folio.circulationbff.domain.dto.Location;
import org.folio.circulationbff.domain.dto.Locations;
import org.folio.circulationbff.support.CqlQuery;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "locations", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface LocationClient extends GetByQueryParamsClient<Locations> {

  @Override
  @GetExchange
  Locations getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @Override
  @GetExchange
  Locations getByQueryParams(@RequestParam Map<String, String> queryParams);

  @GetExchange("/{id}")
  Location findLocation(@PathVariable String id);
}
