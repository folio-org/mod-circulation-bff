package org.folio.circulationbff.client.feign;

import java.util.Map;

import org.folio.circulationbff.domain.dto.ServicePoint;
import org.folio.circulationbff.domain.dto.ServicePoints;
import org.folio.circulationbff.support.CqlQuery;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "service-points", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface ServicePointClient extends GetByQueryParamsClient<ServicePoints> {

  @Override
  @GetExchange
  ServicePoints getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @Override
  @GetExchange
  ServicePoints getByQueryParams(@RequestParam Map<String, String> queryParams);

  @GetExchange("/{id}")
  ServicePoint findServicePoint(@PathVariable String id);
}
