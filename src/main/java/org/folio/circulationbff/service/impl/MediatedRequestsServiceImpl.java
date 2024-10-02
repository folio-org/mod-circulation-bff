package org.folio.circulationbff.service.impl;

import org.folio.circulationbff.client.feign.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.folio.circulationbff.service.MediatedRequestsService;
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
  public ResponseEntity<Void> updateMediatedRequest(MediatedRequest mediatedRequest) {
    log.debug("updateMediatedRequest:: parameters mediatedRequest: {}", mediatedRequest);

    return requestMediatedClient.putRequestMediated(mediatedRequest.getId(), mediatedRequest);
  }

  @Override
  public ResponseEntity<MediatedRequest> saveMediatedRequest(MediatedRequest mediatedRequest) {
    log.debug("saveMediatedRequest:: parameters mediatedRequest: {}", mediatedRequest);

    return requestMediatedClient.postRequestMediated(mediatedRequest);
  }

  @Override
  public ResponseEntity<Void> confirmMediatedRequest(MediatedRequest mediatedRequest) {
    log.debug("confirmMediatedRequest:: parameters mediatedRequest: {}", mediatedRequest);

    return requestMediatedClient.confirmRequestMediated(mediatedRequest.getId(), mediatedRequest);
  }
}
