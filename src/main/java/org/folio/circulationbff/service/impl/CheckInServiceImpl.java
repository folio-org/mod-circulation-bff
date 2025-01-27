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
      log.warn("getEffectiveLocationServicePoint: instance not found");
      return null;
    }
    var itemTenantId = instance.getItems()
      .stream()
      .filter(item -> item.getId().equals(itemId))
      .findFirst()
      .map(SearchItem::getTenantId)
      .orElse(null);
    var tenantId = instance.getItems()
      .stream()
      .filter(item -> item.getId().equals(itemId))
      .findFirst()
      .map(SearchItem::getTenantId)
      .orElse(null);

    if (Objects.equals(tenantId, userTenantsService.getCurrentTenant())) {
      log.info("getEffectiveLocationServicePoint: same tenant case {}", tenantId);
      return fetchServicePointName(itemId);
    } else {
      log.info("getEffectiveLocationServicePoint: cross tenant case {}", tenantId);
      return executionService.executeSystemUserScoped(tenantId, () -> fetchServicePointName(itemId));
    }
  }
  
  private String fetchServicePointName(String itemId) {
    var item = inventoryService.fetchItem(itemId);
    if (item == null) {
      log.warn("fetchServicePointName:: item not found, itemId: {}", itemId);
      return null;
    }
    var location = inventoryService.fetchLocation(item.getEffectiveLocationId());
    if (location == null) {
      log.warn("fetchServicePointName:: location not found, locationId: {}",
        item.getEffectiveLocationId());
      return null;
    }
    var servicePoint = inventoryService.fetchServicePoint(location.getPrimaryServicePoint().toString());
    if (servicePoint == null) {
      log.warn("fetchServicePointName:: servicePoint not found, servicePointId: {}",
        location.getPrimaryServicePoint());
      return null;
    }
    String servicePointName = servicePoint.getName();
    log.info("fetchServicePointName:: result: {}", servicePointName);

    return servicePointName;
  }
      log.info("getEffectiveLocationServicePoint: same tenant case {}", itemTenantId);
      var item = inventoryService.fetchItem(itemId);
      var location = inventoryService.fetchLocation(item.getEffectiveLocationId());
      var servicePoint = inventoryService.fetchServicePoint(location.getPrimaryServicePoint().toString());
      return servicePoint.getName();
    } else {
      log.info("getEffectiveLocationServicePoint: cross tenant case {}", itemTenantId);
      var item = executionService.executeSystemUserScoped(itemTenantId,
        () -> inventoryService.fetchItem(itemId)
      );
      var location = executionService.executeSystemUserScoped(itemTenantId,
        () -> inventoryService.fetchLocation(item.getEffectiveLocationId())
      );
      var servicePoint = executionService.executeSystemUserScoped(itemTenantId,
        () -> inventoryService.fetchServicePoint(location.getPrimaryServicePoint().toString())
      );
      return servicePoint.getName();
    }
  }

}
