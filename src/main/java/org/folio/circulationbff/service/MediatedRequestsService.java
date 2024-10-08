package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.springframework.http.ResponseEntity;

public interface MediatedRequestsService {
  ResponseEntity<Void> updateMediatedRequest(MediatedRequest mediatedRequest);
  ResponseEntity<MediatedRequest> saveMediatedRequest(MediatedRequest mediatedRequest);
  ResponseEntity<Void> confirmMediatedRequest(MediatedRequest mediatedRequest);
}
