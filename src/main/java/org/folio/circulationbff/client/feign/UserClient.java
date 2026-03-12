package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.User;
import org.folio.circulationbff.domain.dto.UserCollection;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "users", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface UserClient {

  @GetExchange("/{userId}")
  User getUser(@PathVariable String userId);

  @GetExchange
  UserCollection getUsersByQuery(@RequestParam("query") String query);

}
