package org.folio.circulationbff.client;

import org.folio.circulationbff.domain.dto.Campus;
import org.folio.circulationbff.domain.dto.Campuses;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "location-units/campuses", contentType = MediaType.APPLICATION_JSON_VALUE, accept = MediaType.APPLICATION_JSON_VALUE)
public interface LocationCampusClient extends GetByQueryParamsClient<Campuses> {

  @GetExchange("/{id}")
  Campus findCampus(@PathVariable String id);
}

