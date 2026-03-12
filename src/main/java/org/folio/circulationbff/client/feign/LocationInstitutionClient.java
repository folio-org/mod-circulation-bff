package org.folio.circulationbff.client.feign;

import java.util.Map;

import org.folio.circulationbff.domain.dto.Institution;
import org.folio.circulationbff.domain.dto.Institutions;
import org.folio.circulationbff.support.CqlQuery;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "location-units/institutions", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface LocationInstitutionClient extends GetByQueryParamsClient<Institutions> {

  @Override
  @GetExchange
  Institutions getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @Override
  @GetExchange
  Institutions getByQueryParams(@RequestParam Map<String, String> queryParams);

  @GetExchange("/{id}")
  Institution findInstitution(@PathVariable String id);
}
