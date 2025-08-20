package org.folio.circulationbff.service;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.DeclareItemLostRequest;

public interface DeclareItemLostService {
 void declareItemLost(UUID loanId, DeclareItemLostRequest declareItemLostRequest);
}
