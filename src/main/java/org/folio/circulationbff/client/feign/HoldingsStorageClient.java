package org.folio.circulationbff.client.feign;

import java.util.Map;

import org.folio.circulationbff.domain.dto.HoldingsRecord;
import org.folio.circulationbff.domain.dto.HoldingsRecords;
import org.folio.circulationbff.support.CqlQuery;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "holdings-storage/holdings", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface HoldingsStorageClient extends GetByQueryParamsClient<HoldingsRecords> {

  @Override
  @GetExchange
  HoldingsRecords getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @Override
  @GetExchange
  HoldingsRecords getByQueryParams(@RequestParam Map<String, String> queryParams);

  @GetExchange("/{id}")
  HoldingsRecord findHoldingsRecord(@PathVariable String id);
}
