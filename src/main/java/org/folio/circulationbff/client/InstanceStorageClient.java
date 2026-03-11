package org.folio.circulationbff.client;

import org.folio.circulationbff.domain.dto.Instance;
import org.folio.circulationbff.domain.dto.Instances;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "instance-storage/instances", contentType = MediaType.APPLICATION_JSON_VALUE,
  accept = MediaType.APPLICATION_JSON_VALUE)
public interface InstanceStorageClient extends GetByQueryParamsClient<Instances> {

  @GetExchange("/{id}")
  Instance findInstance(@PathVariable String id);

}
