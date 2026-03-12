package org.folio.circulationbff.client.feign;

import java.util.Map;

import org.folio.circulationbff.domain.dto.Instance;
import org.folio.circulationbff.domain.dto.Instances;
import org.folio.circulationbff.support.CqlQuery;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "instance-storage/instances", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface InstanceStorageClient extends GetByQueryParamsClient<Instances> {

  @Override
  @GetExchange
  Instances getByQuery(@RequestParam CqlQuery query, @RequestParam int limit);

  @Override
  @GetExchange
  Instances getByQueryParams(@RequestParam Map<String, String> queryParams);

  @GetExchange("/{id}")
  Instance findInstance(@PathVariable String id);
}
