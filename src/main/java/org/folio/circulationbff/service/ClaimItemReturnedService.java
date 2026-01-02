package org.folio.circulationbff.service;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.ClaimItemReturnedRequest;

public interface ClaimItemReturnedService {
  void claimItemReturned(UUID loanId, ClaimItemReturnedRequest claimItemReturnedRequest);
}

