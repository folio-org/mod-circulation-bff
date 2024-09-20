package org.folio.circulationbff.service.impl;

import java.util.UUID;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.service.CirculationBffService;
import org.folio.circulationbff.service.SettingsService;
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

  @Override
  public AllowedServicePoints getAllowedServicePoints(UUID patronGroupId, String operation, UUID instanceId, UUID requestId) {
    log.info("getAllowedServicePoints:: patronGroupId={}, operation={}, instanceId={}, requestId={}",
      patronGroupId, operation, instanceId, requestId);
    if (settingsService.isEcsTlrFeatureEnabled()) {
      log.info("getAllowedServicePoints:: Ecs TLR Feature is enabled. Getting allowed service " +
        "points from mod-tlr module");
      return ecsTlrClient.getAllowedServicePoints(operation, instanceId,
        requestId);
    } else {
      log.info("getAllowedServicePoints:: Ecs TLR Feature is disabled. Getting allowed service " +
        "points from mod-circulation module");
      return circulationClient.allowedServicePoints(patronGroupId, instanceId,
        operation, requestId);
    }
  }
}
