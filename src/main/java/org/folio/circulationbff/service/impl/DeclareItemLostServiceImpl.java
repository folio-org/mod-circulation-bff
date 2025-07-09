package org.folio.circulationbff.service.impl;

import java.util.UUID;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.client.feign.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.DeclareItemLostRequest;
import org.folio.circulationbff.domain.dto.TlrDeclareItemLostRequest;
import org.folio.circulationbff.service.DeclareItemLostService;
import org.folio.circulationbff.service.SettingsService;
import org.folio.circulationbff.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class DeclareItemLostServiceImpl implements DeclareItemLostService {

  private final SettingsService settingsService;
  private final TenantService tenantService;
  private final EcsTlrClient ecsTlrClient;
  private final CirculationClient circulationClient;
  private final RequestMediatedClient requestMediatedClient;

  @Override
  public ResponseEntity<Void> declareItemLost(UUID loanId, DeclareItemLostRequest itemLostRequest) {
    log.info("declareItemLost:: loanId: {}, declareItemLostRequest: {}", () -> loanId,
      () -> itemLostRequest);

    String currentTenantId = tenantService.getCurrentTenantId();

    if (!settingsService.isEcsTlrFeatureEnabled(currentTenantId)) {
      log.info("declareItemLost:: ECS TLR feature is not enabled for tenant: {}, using local service", currentTenantId);
      return circulationClient.declareItemLost(loanId, itemLostRequest);
    }

    log.info("declareItemLost:: ECS TLR feature is enabled for tenant: {}", currentTenantId);

    if (tenantService.isCentralTenant(currentTenantId)) {
      log.info("declareItemLost:: doing declare item lost in central tenant");

      return ecsTlrClient.declareItemLost(new TlrDeclareItemLostRequest()
        .declaredLostDateTime(itemLostRequest.getDeclaredLostDateTime())
        .comment(itemLostRequest.getComment())
        .servicePointId(itemLostRequest.getServicePointId())
        .loanId(loanId));
    }

    if (tenantService.isCurrentTenantSecure()) {
      log.info("declareItemLost:: doing declare item lost in secure tenant");
      return requestMediatedClient.declareItemLost(loanId, itemLostRequest);
    }

    log.info("declareItemLost:: tenant is not central or secure, using local service");
    return circulationClient.declareItemLost(loanId, itemLostRequest);
  }
}
