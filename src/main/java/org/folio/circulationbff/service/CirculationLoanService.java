package org.folio.circulationbff.service;

import java.util.UUID;
import org.folio.circulationbff.domain.dto.CirculationLoan;
import org.folio.circulationbff.domain.dto.CirculationLoans;

public interface CirculationLoanService {

  /**
   * Find loans by query string with offset
   *
   * @param query - CQL query string
   * @param limit - a limit of records to be returned
   * @param offset - a number of records to be skipped
   * @return Loans - a collection of circulation loans
   */
  CirculationLoans findCirculationLoans(String query, Integer limit, Integer offset, String totalRecords);

  /**
   * Find loan by id.
   *
   * @param loanId - a loan identifier
   * @return a circulation loan object
   */
  CirculationLoan getCirculationLoanById(UUID loanId);
}
