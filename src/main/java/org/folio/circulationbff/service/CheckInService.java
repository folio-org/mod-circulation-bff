package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.CheckInRequest;
import org.folio.circulationbff.domain.dto.CheckInResponse;

public interface CheckInService {
  CheckInResponse checkIn(CheckInRequest request);
}
