package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.User;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "users", url = "users", configuration = FeignClientConfiguration.class)
public interface UserClient {

  @GetMapping
  User getExternalUserByQuery(@RequestParam("query") String query);

}