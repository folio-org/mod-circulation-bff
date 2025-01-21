package org.folio.circulationbff.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.circulationbff.client.feign.ItemStorageClient;
import org.folio.circulationbff.client.feign.ServicePointClient;
import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.ServicePoint;
import org.folio.circulationbff.service.InventoryService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class InventoryServiceImpl implements InventoryService {
  private final ItemStorageClient itemClient;
  private final ServicePointClient servicePointClient;

  @Override
  public Item fetchItem(String id) {
    log.info("fetchItem:: fetching item {}", id);
    return itemClient.findItem(id);
  }

  @Override
  public ServicePoint fetchServicePoint(String id) {
    log.info("fetchServicePoint:: fetching service point {}", id);
    return servicePointClient.findServicePoint(id);
  }
}
