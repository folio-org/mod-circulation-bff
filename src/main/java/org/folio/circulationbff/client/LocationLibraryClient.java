package org.folio.circulationbff.client;

import java.util.Map;

import org.folio.circulationbff.domain.dto.Libraries;
import org.folio.circulationbff.domain.dto.Library;
import org.folio.circulationbff.support.CqlQuery;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "location-units/libraries", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface LocationLibraryClient extends GetByQueryParamsClient<Libraries> {

  @Override
  @GetExchange
  Libraries getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @Override
  @GetExchange
  Libraries getByQueryParams(@RequestParam Map<String, String> queryParams);

  @GetExchange("/{id}")
  Library findLibrary(@PathVariable String id);
}
