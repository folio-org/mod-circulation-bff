package org.folio.circulationbff.service;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.DeclareClaimedReturnedItemAsMissingRequest;
import org.springframework.http.ResponseEntity;

public interface DeclareClaimedReturnedItemAsMissingService {
  ResponseEntity<Void> declareClaimedReturnedItemAsMissing(UUID loanId,
    DeclareClaimedReturnedItemAsMissingRequest declareClaimedReturnedItemAsMissingRequest);
}
