package org.folio.circulationbff.client;

import org.folio.circulationbff.domain.dto.CheckOutRequest;
import org.folio.circulationbff.domain.dto.CheckOutResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange(url = "circulation/check-out-by-barcode",
  contentType = MediaType.APPLICATION_JSON_VALUE, accept = MediaType.APPLICATION_JSON_VALUE)
public interface CheckOutClient {

  @PostExchange
  CheckOutResponse checkOut(@RequestBody CheckOutRequest request);

}
