package org.folio.circulationbff.service.impl;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.domain.dto.CheckInRequest;
import org.folio.circulationbff.domain.dto.CheckInResponse;
import org.folio.circulationbff.service.CheckInService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class CheckInServiceImpl implements CheckInService {

  private final CirculationClient circulationClient;

  @Override
  public CheckInResponse checkIn(CheckInRequest request) {
    log.info("checkIn: checking in item with barcode {} on service point {}",
      request::getItemBarcode, request::getServicePointId);
    return circulationClient.checkIn(request);
  }

}
