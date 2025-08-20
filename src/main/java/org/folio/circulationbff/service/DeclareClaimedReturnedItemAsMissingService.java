package org.folio.circulationbff.service;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.DeclareClaimedReturnedItemAsMissingRequest;

public interface DeclareClaimedReturnedItemAsMissingService {
  void declareClaimedReturnedItemAsMissing(UUID loanId,
    DeclareClaimedReturnedItemAsMissingRequest declareClaimedReturnedItemAsMissingRequest);
}
