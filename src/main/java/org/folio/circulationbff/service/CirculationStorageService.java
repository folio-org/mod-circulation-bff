package org.folio.circulationbff.service;

import java.util.Collection;
import java.util.Optional;

import org.folio.circulationbff.domain.dto.Loan;
import org.folio.circulationbff.support.CqlQuery;

public interface CirculationStorageService {
  Collection<Loan> findLoans(CqlQuery query, int limit);
  Optional<Loan> findOpenLoan(String itemId);
}
