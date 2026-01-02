package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.AllowedServicePointParams;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.BffRequest;
import org.folio.circulationbff.domain.dto.PickSlipCollection;
import org.folio.circulationbff.domain.dto.Request;
import org.folio.circulationbff.domain.dto.Requests;
import org.folio.circulationbff.domain.dto.SearchSlipCollection;

public interface CirculationBffService {
  AllowedServicePoints getAllowedServicePoints(AllowedServicePointParams allowedServicePointParams, String tenantId);
  Request createRequest(BffRequest request, String tenantId);
  PickSlipCollection fetchPickSlipsByServicePointId(String servicePointId);
  SearchSlipCollection fetchSearchSlipsByServicePointId(String servicePointId);
  Requests getBatchRequestInfoEnrichedRequests(String query, Integer offset, Integer limit, String totalRecords);
}
