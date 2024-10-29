package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.AllowedServicePointParams;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.CirculationSettingsResponse;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "circulation", url = "circulation",
  configuration = FeignClientConfiguration.class)
public interface CirculationClient {

  @GetMapping("/requests/allowed-service-points")
  AllowedServicePoints allowedServicePoints (@SpringQueryMap AllowedServicePointParams params);

  @GetMapping(value = "/settings")
  CirculationSettingsResponse getCirculationSettingsByQuery(@RequestParam("query") String query);
}
