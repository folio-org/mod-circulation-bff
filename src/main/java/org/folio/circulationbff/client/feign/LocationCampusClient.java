package org.folio.circulationbff.client.feign;

import java.util.Map;

import org.folio.circulationbff.domain.dto.Campus;
import org.folio.circulationbff.domain.dto.Campuses;
import org.folio.circulationbff.support.CqlQuery;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "location-units/campuses", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface LocationCampusClient extends GetByQueryParamsClient<Campuses> {

  @Override
  @GetExchange
  Campuses getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @Override
  @GetExchange
  Campuses getByQueryParams(@RequestParam Map<String, String> queryParams);

  @GetExchange("/{id}")
  Campus findCampus(@PathVariable String id);
}
