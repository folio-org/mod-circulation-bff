package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.AllowedServicePointParams;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.BffRequest;
import org.folio.circulationbff.domain.dto.Request;
import org.folio.circulationbff.domain.dto.StaffSlipsCollection;

public interface CirculationBffService {
  AllowedServicePoints getAllowedServicePoints(AllowedServicePointParams allowedServicePointParams, String tenantId);
  Request createRequest(BffRequest request, String tenantId);
  StaffSlipsCollection pickStaffSlipsByServicePointId(String servicePointId);
  StaffSlipsCollection searchStaffSlipsByServicePointId(String servicePointId);
}
