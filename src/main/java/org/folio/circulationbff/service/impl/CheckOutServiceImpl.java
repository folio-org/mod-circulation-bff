package org.folio.circulationbff.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.circulationbff.client.feign.CheckOutClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.domain.dto.CheckOutRequest;
import org.folio.circulationbff.domain.dto.CheckOutResponse;
import org.folio.circulationbff.service.CheckOutService;
import org.folio.circulationbff.service.SettingsService;
import org.folio.circulationbff.service.TenantService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class CheckOutServiceImpl implements CheckOutService {

  private final SettingsService settingsService;
  private final TenantService tenantService;
  private final CheckOutClient checkOutClient;
  private final EcsTlrClient ecsTlrClient;
  private final SystemUserScopedExecutionService systemUserService;

  @Override
  public CheckOutResponse checkOut(CheckOutRequest request) {
    log.info("checkOut: checking out item with barcode {} from service point {}",
      request.getItemBarcode(), request.getServicePointId());

    if (settingsService.isEcsTlrFeatureEnabled()) {
      String currentTenantId = tenantService.getCurrentTenantId();
      String centralTenantId = tenantService.getCentralTenantId().orElseThrow();
      if (currentTenantId.equals(centralTenantId)) {
        log.info("checkOut:: doing ECS checkout for central tenant");
        return ecsTlrClient.checkOutByBarcode(request.targetTenantId(centralTenantId));
      } else if (tenantService.isSecureTenant(currentTenantId)) {
        log.info("checkOut:: doing ECS checkout for secure tenant");
        return systemUserService.executeSystemUserScoped(centralTenantId,
          () -> ecsTlrClient.checkOutByBarcode(request.targetTenantId(currentTenantId)));
      }
    }

    log.info("checkOut: doing regular checkout in local tenant");
    return checkOutClient.checkOut(request);
  }
}
