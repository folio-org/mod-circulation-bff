package org.folio.circulationbff.service.impl;

import java.util.UUID;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.service.CirculationBffService;
import org.folio.circulationbff.service.SettingsService;
import org.folio.circulationbff.service.UserTenantsService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class CirculationBffServiceImpl implements CirculationBffService {

  private final CirculationClient circulationClient;
  private final EcsTlrClient ecsTlrClient;
  private final SettingsService settingsService;
  private final UserTenantsService userTenantsService;
  @Override
  public AllowedServicePoints getAllowedServicePoints(String tenantId, UUID patronGroupId,
                                                      String operation,
                                                      UUID instanceId, UUID requestId,
                                                      UUID requesterId, UUID itemId) {
    log.info("getAllowedServicePoints:: params: patronGroupId={}, operation={}, instanceId={}, " +
      "requestId={}, requesterId={}, itemId={}", patronGroupId, operation, instanceId, requestId,
      requesterId, itemId);
    if (settingsService.isEcsTlrFeatureEnabled(tenantId) && userTenantsService.isCentralTenant(tenantId)) {
      log.info("getAllowedServicePoints:: Ecs TLR Feature is enabled. Getting allowed service " +
        "points from mod-tlr module");
      return ecsTlrClient.getAllowedServicePoints(operation, requesterId, instanceId, requestId,
        itemId);
    } else {
      log.info("getAllowedServicePoints:: Ecs TLR Feature is disabled. Getting allowed service " +
        "points from mod-circulation module");
      return circulationClient.allowedServicePoints(operation, requesterId,
        instanceId, itemId, requestId, patronGroupId);
    }
  }
}
