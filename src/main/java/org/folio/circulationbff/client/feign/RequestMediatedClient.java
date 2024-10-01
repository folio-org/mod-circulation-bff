package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "requests-mediated", url = "requests-mediated", configuration = FeignClientConfiguration.class)
public interface RequestMediatedClient {

  @PutMapping("/mediated-requests/{requestId}")
  ResponseEntity<Void> putRequestMediated(@PathVariable String requestId,
    @RequestBody MediatedRequest mediatedRequest);

  @PostMapping("/mediated-requests/{requestId}/confirm")
  ResponseEntity<Void> confirmRequestMediated(@PathVariable String requestId,
    @RequestBody MediatedRequest mediatedRequest);
}
