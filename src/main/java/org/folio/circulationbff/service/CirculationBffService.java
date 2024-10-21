package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.AllowedServicePointParams;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.BffRequest;

public interface CirculationBffService {
  AllowedServicePoints getAllowedServicePoints(AllowedServicePointParams allowedServicePointParams, String tenantId);
  BffRequest createRequest(BffRequest request, String tenantId);
}
