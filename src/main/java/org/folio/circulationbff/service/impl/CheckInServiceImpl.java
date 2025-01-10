package org.folio.circulationbff.service.impl;

import org.folio.circulationbff.domain.dto.CheckInRequest;
import org.folio.circulationbff.domain.dto.CheckInResponse;
import org.folio.circulationbff.service.CheckInService;
import org.springframework.stereotype.Service;

@Service
public class CheckInServiceImpl implements CheckInService {

  @Override
  public CheckInResponse checkIn(CheckInRequest request) {
    return new CheckInResponse().itemBarcode(request.getItemBarcode());
  }

}
