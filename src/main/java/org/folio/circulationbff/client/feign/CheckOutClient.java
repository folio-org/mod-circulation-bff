package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.client.feign.config.ErrorForwardingFeignClientConfiguration;
import org.folio.circulationbff.domain.dto.CheckOutRequest;
import org.folio.circulationbff.domain.dto.CheckOutResponse;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "check-out", url = "circulation/check-out-by-barcode",
  configuration = {FeignClientConfiguration.class, ErrorForwardingFeignClientConfiguration.class})
public interface CheckOutClient {

  @PostMapping
  CheckOutResponse checkOut(@RequestBody CheckOutRequest request);
}
