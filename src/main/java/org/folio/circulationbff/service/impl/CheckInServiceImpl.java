package org.folio.circulationbff.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.circulationbff.client.feign.CheckInClient;
import org.folio.circulationbff.domain.dto.CheckInRequest;
import org.folio.circulationbff.domain.dto.CheckInResponse;
import org.folio.circulationbff.domain.dto.SearchItem;
import org.folio.circulationbff.service.CheckInService;
import org.folio.circulationbff.service.InventoryService;
import org.folio.circulationbff.service.SearchService;
import org.folio.circulationbff.service.UserTenantsService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Log4j2
public class CheckInServiceImpl implements CheckInService {

  private final CheckInClient checkInClient;
  private final SearchService searchService;
  private final UserTenantsService userTenantsService;
  private final InventoryService inventoryService;
  private final SystemUserScopedExecutionService executionService;

  @Override
  public CheckInResponse checkIn(CheckInRequest request) {
    log.info("checkIn: checking in item with barcode {} on service point {}",
      request.getItemBarcode(), request.getServicePointId());
    var response = checkInClient.checkIn(request);
    var item = response.getItem();
    var servicePointName = getEffectiveLocationServicePoint(item.getId());
    if (servicePointName != null) {
      var slipContextItem = response.getStaffSlipContext().getItem();
      slipContextItem.setEffectiveLocationPrimaryServicePointName(servicePointName);
      slipContextItem.toServicePoint(servicePointName);
    }
    return response;
  }

  private String getEffectiveLocationServicePoint(String itemId) {
    log.info("getEffectiveLocationServicePoint: itemId {}", itemId);
    var instance = searchService.findInstanceByItemId(itemId);
    if (instance == null) {
      log.info("getEffectiveLocationServicePoint: instance not found");
      return null;
    }
    var itemTenantId = instance.getItems()
      .stream()
      .filter(item -> item.getId().equals(itemId))
      .findFirst()
      .map(SearchItem::getTenantId)
      .orElse(null);
    if (Objects.equals(itemTenantId, userTenantsService.getCurrentTenant())) {
      log.info("getEffectiveLocationServicePoint: same tenant case {}", itemTenantId);
      var item = inventoryService.fetchItem(itemId);
      var servicePoint = inventoryService.fetchServicePoint(item.getEffectiveLocationId());
      return servicePoint.getName();
    } else {
      log.info("getEffectiveLocationServicePoint: cross tenant case {}", itemTenantId);
      var item = executionService.executeSystemUserScoped(
        itemTenantId,
        () -> inventoryService.fetchItem(itemId)
      );
      var servicePoint = executionService.executeSystemUserScoped(
        itemTenantId,
        () -> inventoryService.fetchServicePoint(item.getEffectiveLocationId())
      );
      return servicePoint.getName();
    }
  }

}
