package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.AllowedServicePointParams;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.BffRequest;
import org.folio.circulationbff.domain.dto.Request;
import org.folio.circulationbff.domain.dto.SlipsCollection;

public interface CirculationBffService {
  AllowedServicePoints getAllowedServicePoints(AllowedServicePointParams allowedServicePointParams, String tenantId);
  Request createRequest(BffRequest request, String tenantId);
  SlipsCollection fetchPickSlipsByServicePointId(String servicePointId);
  SlipsCollection fetchSearchSlipsByServicePointId(String servicePointId);
}
