package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.AllowedServicePointParams;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.BffRequest;
import org.folio.circulationbff.domain.dto.Request;

public interface CirculationBffService {
  AllowedServicePoints getAllowedServicePoints(AllowedServicePointParams allowedServicePointParams, String tenantId);
  Request createRequest(BffRequest request, String tenantId);
}
