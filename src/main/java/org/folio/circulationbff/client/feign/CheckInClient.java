package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.client.feign.config.ErrorForwardingFeignClientConfiguration;
import org.folio.circulationbff.domain.dto.CheckInRequest;
import org.folio.circulationbff.domain.dto.CheckInResponse;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "check-in", url = "circulation/check-in-by-barcode",
  configuration = { FeignClientConfiguration.class, ErrorForwardingFeignClientConfiguration.class })
public interface CheckInClient {

  @PostMapping
  CheckInResponse checkIn(@RequestBody CheckInRequest request);
}
