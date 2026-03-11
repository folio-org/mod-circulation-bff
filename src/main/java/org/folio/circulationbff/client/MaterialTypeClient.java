package org.folio.circulationbff.client;

import org.folio.circulationbff.domain.dto.MaterialType;
import org.folio.circulationbff.domain.dto.MaterialTypes;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "material-types", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface MaterialTypeClient extends GetByQueryParamsClient<MaterialTypes> {

  @GetExchange("/{id}")
  MaterialType findMaterialType(@PathVariable String id);

}
