package org.folio.circulationbff.client;

import org.folio.circulationbff.domain.dto.LoanType;
import org.folio.circulationbff.domain.dto.LoanTypes;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "loan-types", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface LoanTypeClient extends GetByQueryParamsClient<LoanTypes> {

  @GetExchange("/{id}")
  LoanType findLoanType(@PathVariable String id);

}
