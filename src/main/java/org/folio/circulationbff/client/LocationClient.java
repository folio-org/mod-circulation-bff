package org.folio.circulationbff.client;

import org.folio.circulationbff.domain.dto.Location;
import org.folio.circulationbff.domain.dto.Locations;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "locations", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface LocationClient extends GetByQueryParamsClient<Locations> {

  @GetExchange("/{id}")
  Location findLocation(@PathVariable String id);

}
