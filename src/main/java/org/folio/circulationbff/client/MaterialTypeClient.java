package org.folio.circulationbff.client;

import java.util.Map;

import org.folio.circulationbff.domain.dto.MaterialType;
import org.folio.circulationbff.domain.dto.MaterialTypes;
import org.folio.circulationbff.support.CqlQuery;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "material-types", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface MaterialTypeClient extends GetByQueryParamsClient<MaterialTypes> {

  @Override
  @GetExchange
  MaterialTypes getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @Override
  @GetExchange
  MaterialTypes getByQueryParams(@RequestParam Map<String, String> queryParams);

  @GetExchange("/{id}")
  MaterialType findMaterialType(@PathVariable String id);
}
