package org.folio.circulationbff.client;

import org.folio.circulationbff.domain.dto.ServicePoint;
import org.folio.circulationbff.domain.dto.ServicePoints;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "service-points", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface ServicePointClient extends GetByQueryParamsClient<ServicePoints> {

  @GetExchange("/{id}")
  ServicePoint findServicePoint(@PathVariable String id);

}
