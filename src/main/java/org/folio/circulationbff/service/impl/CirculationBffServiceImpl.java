package org.folio.circulationbff.service.impl;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.domain.dto.AllowedServicePointParams;
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
  public AllowedServicePoints getAllowedServicePoints(AllowedServicePointParams params, String tenantId) {
    log.info("getAllowedServicePoints:: params: {}", params);
    if (settingsService.isEcsTlrFeatureEnabled(tenantId) && userTenantsService.isCentralTenant(tenantId)) {
      log.info("getAllowedServicePoints:: Ecs TLR Feature is enabled. Getting allowed service " +
        "points from mod-tlr module");
      return ecsTlrClient.getAllowedServicePoints(params.getOperation(), params.getRequesterId(),
        params.getInstanceId(), params.getRequestId(), params.getItemId());
    } else {
      log.info("getAllowedServicePoints:: Ecs TLR Feature is disabled. Getting allowed service " +
        "points from mod-circulation module");
      return circulationClient.allowedServicePoints(params.getOperation(), params.getRequesterId(),
        params.getInstanceId(), params.getItemId(), params.getRequestId(), params.getPatronGroupId());
    }
  }
}
