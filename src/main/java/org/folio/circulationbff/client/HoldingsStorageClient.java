package org.folio.circulationbff.client;

import org.folio.circulationbff.domain.dto.HoldingsRecord;
import org.folio.circulationbff.domain.dto.HoldingsRecords;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "holdings-storage/holdings", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface HoldingsStorageClient extends GetByQueryParamsClient<HoldingsRecords> {

  @GetExchange("/{id}")
  HoldingsRecord findHoldingsRecord(@PathVariable String id);

}
