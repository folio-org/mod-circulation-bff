package org.folio.circulationbff.service;

import java.util.Collection;
import java.util.Map;

import org.folio.circulationbff.domain.dto.LoanItem;
import org.folio.circulationbff.domain.model.LoanEnrichment;

public interface LoanEnrichmentService {

  /**
   * Resolves item and instance data for DCB loan items without querying instances through
   * mod-search. Items that cannot be resolved are absent from the result.
   *
   * @param loanItems loan items to enrich, may contain duplicates
   * @return enrichment data by item id
   */
  Map<String, LoanEnrichment> enrich(Collection<LoanItem> loanItems);
}
