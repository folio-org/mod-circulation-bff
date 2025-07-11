package org.folio.circulationbff.service;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.DeclareItemLostRequest;
import org.springframework.http.ResponseEntity;

public interface DeclareItemLostService {

  ResponseEntity<Void> declareItemLost(UUID loanId, DeclareItemLostRequest declareItemLostRequest);
}
