package org.folio.circulationbff.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.circulationbff.client.feign.CheckOutClient;
import org.folio.circulationbff.domain.dto.CheckOutRequest;
import org.folio.circulationbff.domain.dto.CheckOutResponse;
import org.folio.circulationbff.service.CheckOutService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class CheckOutServiceImpl implements CheckOutService {

  private final CheckOutClient checkOutClient;

  @Override
  public CheckOutResponse checkOut(CheckOutRequest request) {
    log.info("checkOut: checking out item with barcode {} from service point {}",
      request.getItemBarcode(), request.getServicePointId());
    return checkOutClient.checkOut(request);
  }
}
