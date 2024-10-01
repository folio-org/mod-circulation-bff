package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.springframework.http.ResponseEntity;

public interface MediatedRequestsService {
  ResponseEntity<Void> updateAndConfirmMediatedRequest(MediatedRequest mediatedRequest);
}
