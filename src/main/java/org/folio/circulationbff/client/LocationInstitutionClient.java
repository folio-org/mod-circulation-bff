package org.folio.circulationbff.client;

import org.folio.circulationbff.domain.dto.Institution;
import org.folio.circulationbff.domain.dto.Institutions;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "location-units/institutions", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface LocationInstitutionClient extends GetByQueryParamsClient<Institutions> {

  @GetExchange("/{id}")
  Institution findInstitution(@PathVariable String id);

}

