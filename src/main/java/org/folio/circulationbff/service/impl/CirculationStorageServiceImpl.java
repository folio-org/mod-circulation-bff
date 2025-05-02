package org.folio.circulationbff.service.impl;

import java.util.Collection;
import java.util.Optional;

import org.folio.circulationbff.client.feign.LoanStorageClient;
import org.folio.circulationbff.domain.dto.Loan;
import org.folio.circulationbff.service.CirculationStorageService;
import org.folio.circulationbff.support.CqlQuery;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class CirculationStorageServiceImpl implements CirculationStorageService {

  private static final String LOAN_STATUS_OPEN = "Open";
  private final LoanStorageClient loanStorageClient;

  @Override
  public Collection<Loan> findLoans(CqlQuery query, int limit) {
    log.info("findLoans:: fetching loans by query: {}", query);
    Collection<Loan> loans = loanStorageClient.getByQuery(query, limit)
        .getLoans();
    log.info("findLoans:: found {} loans", loans.size());
    return loans;
  }

  @Override
  public Optional<Loan> findOpenLoan(String itemId) {
    CqlQuery query = CqlQuery.exactMatch("itemId", itemId)
      .and(CqlQuery.exactMatch("status.name", LOAN_STATUS_OPEN));

    return findLoans(query, 1)
      .stream()
      .findFirst();
  }
}
