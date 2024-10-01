package org.folio.circulationbff.service.impl;

import org.folio.circulationbff.client.feign.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.folio.circulationbff.service.MediatedRequestsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class MediatedRequestsServiceImpl implements MediatedRequestsService {
  private final RequestMediatedClient requestMediatedClient;

  @Override
  public ResponseEntity<Void> updateAndConfirmMediatedRequest(MediatedRequest mediatedRequest) {
    log.debug("updateAndConfirmMediatedRequest:: parameters mediatedRequest: {}", mediatedRequest);
    ResponseEntity<Void> putResponse = requestMediatedClient.putRequestMediated(
      mediatedRequest.getId(), mediatedRequest);

    if (putResponse.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
      log.info("updateAndConfirmMediatedRequest:: mediatedRequest: {} has been updated",
        mediatedRequest.getId());

      return requestMediatedClient.confirmRequestMediated(mediatedRequest.getId(), mediatedRequest);
    }
    log.warn("updateAndConfirmMediatedRequest:: request: {} has not been updated: {}",
      mediatedRequest.getId(), putResponse.getBody());

    return putResponse;
  }
}
