package org.folio.circulationbff.service.impl;

import java.util.List;

import org.folio.circulationbff.client.feign.InventoryClient;
import org.folio.circulationbff.client.feign.ItemStorageClient;
import org.folio.circulationbff.client.feign.LocationCampusClient;
import org.folio.circulationbff.client.feign.LocationClient;
import org.folio.circulationbff.client.feign.LocationInstitutionClient;
import org.folio.circulationbff.client.feign.LocationLibraryClient;
import org.folio.circulationbff.client.feign.ServicePointClient;
import org.folio.circulationbff.domain.dto.Campus;
import org.folio.circulationbff.domain.dto.Institution;
import org.folio.circulationbff.domain.dto.InventoryItem;
import org.folio.circulationbff.domain.dto.InventoryItems;
import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.Library;
import org.folio.circulationbff.domain.dto.Location;
import org.folio.circulationbff.domain.dto.SearchInstance;
import org.folio.circulationbff.domain.dto.SearchItem;
import org.folio.circulationbff.domain.dto.ServicePoint;
import org.folio.circulationbff.service.InventoryService;
import org.folio.circulationbff.service.SearchService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class InventoryServiceImpl implements InventoryService {
  private final ItemStorageClient itemClient;
  private final LocationClient locationClient;
  private final ServicePointClient servicePointClient;
  private final LocationLibraryClient libraryClient;
  private final LocationInstitutionClient institutionClient;
  private final LocationCampusClient campusClient;
  private final SearchService searchService;
  private final InventoryClient inventoryItemClient;
  private final SystemUserScopedExecutionService systemUserScopedExecutionService;

  private static final String QUOTES_REGEX = "^[\"']|[\"']$";

  @Override
  public Item fetchItem(String id) {
    log.info("fetchItem:: fetching item {}", id);
    return itemClient.findItem(id);
  }

  @Override
  public Location fetchLocation(String id) {
    log.info("fetchLocation:: fetching location {}", id);
    return locationClient.findLocation(id);
  }

  @Override
  public ServicePoint fetchServicePoint(String id) {
    log.info("fetchServicePoint:: fetching service point {}", id);
    return servicePointClient.findServicePoint(id);
  }

  @Override
  public Campus fetchCampus(String id) {
    log.info("fetchCampus:: fetching campus {}", id);
    return campusClient.findCampus(id);
  }

  @Override
  public Institution fetchInstitution(String id) {
    log.info("fetchInstitution:: fetching institution {}", id);
    return institutionClient.findInstitution(id);
  }

  @Override
  public Library fetchLibrary(String id) {
    log.info("fetchLibrary:: fetching library {}", id);
    return libraryClient.findLibrary(id);
  }

  @Override
  public InventoryItems fetchInventoryItemsByQuery(String query) {
    log.info("fetchInventoryItemsByQuery:: fetching by query {}", query);

    String barcode = query
      .replace("barcode==", "")
      .replaceAll(QUOTES_REGEX, "")
      .trim();
    log.info("fetchInventoryItemsByQuery:: extracted barcode from query: {}", barcode);

    return searchService.findInstanceByItemBarcode(barcode)
      .map(SearchInstance::getItems)
      .flatMap(items -> items.stream()
        .filter(item -> barcode.equals(item.getBarcode()))
        .findFirst()
        .map(this::fetchInventoryItem)
        .map(this::singleElementInventoryItems))
      .orElseGet(this::emptyInventoryItems);
  }

  private InventoryItem fetchInventoryItem(SearchItem searchItem) {
    if (searchItem == null || searchItem.getId() == null || searchItem.getTenantId() == null) {
      log.warn("fetchInventoryItem:: searchItem is null or missing id/tenantId");
      return null;
    }

    log.info("fetchInventoryItem:: fetching inventory item {} from tenant {}", searchItem::getId,
      searchItem::getTenantId);

    return systemUserScopedExecutionService.executeSystemUserScoped(searchItem.getTenantId(),
      () -> inventoryItemClient.getById(searchItem.getId()));
  }

  private InventoryItems singleElementInventoryItems(InventoryItem inventoryItem) {
    return new InventoryItems()
      .items(List.of(inventoryItem))
      .totalRecords(1);
  }

  private InventoryItems emptyInventoryItems() {
    return new InventoryItems()
      .items(List.of())
      .totalRecords(0);
  }
}
