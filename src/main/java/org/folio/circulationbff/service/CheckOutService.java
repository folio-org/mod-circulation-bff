package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.CheckOutRequest;
import org.folio.circulationbff.domain.dto.CheckOutResponse;

public interface CheckOutService {
  CheckOutResponse checkOut(CheckOutRequest request);
}
