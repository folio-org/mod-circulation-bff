package org.folio.circulationbff.client;

import org.folio.circulationbff.domain.dto.UserTenantCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "user-tenants", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface UserTenantsClient {

  @GetExchange()
  UserTenantCollection getUserTenants(@RequestParam(name = "limit", required = false) Integer limit);

}
