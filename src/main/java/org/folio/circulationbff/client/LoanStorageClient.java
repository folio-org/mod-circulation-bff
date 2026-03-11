package org.folio.circulationbff.client;

import org.folio.circulationbff.domain.dto.Loans;
import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "loan-storage/loans", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface LoanStorageClient extends GetByQueryParamsClient<Loans> {

}
