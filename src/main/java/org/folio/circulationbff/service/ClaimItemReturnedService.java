package org.folio.circulationbff.service;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.ClaimItemReturnedRequest;
import org.springframework.http.ResponseEntity;

public interface ClaimItemReturnedService {

  ResponseEntity<Void> claimItemReturned(UUID loanId, ClaimItemReturnedRequest claimItemReturnedRequest);
}

