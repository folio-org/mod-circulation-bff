package org.folio.circulationbff.client;

import org.folio.circulationbff.domain.dto.Libraries;
import org.folio.circulationbff.domain.dto.Library;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "location-units/libraries", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface LocationLibraryClient extends GetByQueryParamsClient<Libraries> {

  @GetExchange("/{id}")
  Library findLibrary(@PathVariable String id);

}
