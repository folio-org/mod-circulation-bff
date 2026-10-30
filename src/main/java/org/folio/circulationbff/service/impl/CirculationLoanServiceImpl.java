package org.folio.circulationbff.service.impl;

import static java.lang.Boolean.TRUE;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.circulationbff.client.CirculationClient;
import org.folio.circulationbff.domain.dto.CirculationLoan;
import org.folio.circulationbff.domain.dto.CirculationLoans;
import org.folio.circulationbff.domain.dto.LoanItem;
import org.folio.circulationbff.domain.dto.Parameter;
import org.folio.circulationbff.domain.mapping.CirculationLoanMapper;
import org.folio.circulationbff.domain.model.LoanEnrichment;
import org.folio.circulationbff.domain.type.ErrorCode;
import org.folio.circulationbff.exception.ValidationException;
import org.folio.circulationbff.service.CirculationLoanService;
import org.folio.circulationbff.service.LoanEnrichmentService;
import org.folio.circulationbff.service.SettingsService;
import org.folio.circulationbff.service.TenantService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class CirculationLoanServiceImpl implements CirculationLoanService {

  private final LoanEnrichmentService loanEnrichmentService;
  private final TenantService tenantService;
  private final SettingsService settingsService;
  private final CirculationClient circulationClient;
  private final CirculationLoanMapper circulationLoanMapper;

  @Value("${folio.loans.max-limit}")
  private int loansMaxLimit;

  @Override
  public CirculationLoans findCirculationLoans(String query, Integer limit,
    Integer offset, String totalRecords) {

    validateLimit(limit);
    log.info("findCirculationLoans:: enriching circulation loans by parameters: " +
      "query={}, limit={}, offset={}, totalRecords={}", query, limit, offset, totalRecords);
    var circulationLoans = circulationClient.findLoansByQuery(query, limit, offset, totalRecords);
    var dcbLoanItems = circulationLoans.getLoans().stream()
      .filter(CirculationLoanServiceImpl::shouldEnrichCirculationLoan)
      .map(CirculationLoan::getItem)
      .toList();

    if (CollectionUtils.isEmpty(dcbLoanItems) || !isEnrichOperationAllowed()) {
      var cause = "(DCB items not found | not tenant central | TLR disabled)";
      log.info("findCirculationLoans:: circulation loans not enriched: {}", cause);
      return circulationLoans;
    }

    var enrichments = loanEnrichmentService.enrich(dcbLoanItems);
    var enrichedLoanIds = new ArrayList<String>();

    for (var loan : circulationLoans.getLoans()) {
      if (shouldEnrichCirculationLoan(loan)) {
        var enrichment = enrichments.get(loan.getItemId());
        if (enrichment == null) {
          log.warn("findCirculationLoans:: enrichment not found by itemId: {}", loan.getItemId());
          continue;
        }

        enrichedLoanIds.add(loan.getId());
        loan.setItem(enrichLoanItem(loan.getItem(), enrichment));
      } else {
        log.debug("findCirculationLoans:: circulation loan not enriched: {}", loan.getId());
      }
    }

    log.info("findCirculationLoans:: circulation loans are enriched: {} of {}",
      enrichedLoanIds.size(), circulationLoans.getLoans().size());

    return circulationLoans;
  }

  @Override
  public CirculationLoan getCirculationLoanById(UUID loanId) {
    log.info("getCirculationLoanById:: enriching circulation loan by id: {}", loanId);
    var circulationLoan = circulationClient.findLoanById(loanId);
    if (!(shouldEnrichCirculationLoan(circulationLoan) && isEnrichOperationAllowed())) {
      var cause = "(non DCB item | not tenant central | TLR disabled)";
      log.info("getCirculationLoanById:: circulation loan not enriched ({}): {}", cause, loanId);
      return circulationLoan;
    }

    var itemId = circulationLoan.getItemId();
    var enrichment = loanEnrichmentService.enrich(List.of(circulationLoan.getItem())).get(itemId);
    if (enrichment == null) {
      log.warn("getCirculationLoanById:: enrichment not found by itemId: {}", itemId);
      return circulationLoan;
    }

    log.info("getCirculationLoanById:: circulation loan is enriched: {}", loanId);
    circulationLoan.setItem(enrichLoanItem(circulationLoan.getItem(), enrichment));
    return circulationLoan;
  }

  private void validateLimit(Integer limit) {
    if (limit != null && limit > loansMaxLimit) {
      log.warn("validateLimit:: requested limit {} exceeds the maximum {}", limit, loansMaxLimit);
      throw new ValidationException(
        "Requested limit exceeds the maximum allowed value of " + loansMaxLimit,
        ErrorCode.LOANS_LIMIT_EXCEEDED,
        List.of(new Parameter().key("limit").value(String.valueOf(limit))));
    }
  }

  private boolean isEnrichOperationAllowed() {
    return settingsService.isEcsTlrFeatureEnabled() &&
      (tenantService.isCurrentTenantCentral() || tenantService.isCurrentTenantSecure());
  }

  private static boolean shouldEnrichCirculationLoan(CirculationLoan loan) {
    return TRUE.equals(loan.getIsDcb()) && loan.getItem() != null;
  }

  private LoanItem enrichLoanItem(LoanItem loanItem, LoanEnrichment enrichment) {
    return circulationLoanMapper.enrichLoanItem(loanItem, enrichment);
  }
}
