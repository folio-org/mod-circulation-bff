package org.folio.circulationbff.service.impl;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.service.SettingsService;
import org.folio.circulationbff.service.TenantService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class SettingsServiceImpl implements SettingsService {

  public static final String ECS_TLR_FEATURE_SETTINGS = "name=ecsTlrFeature";
  private final EcsTlrClient ecsTlrClient;
  private final CirculationClient circulationClient;
  private final TenantService tenantService;

  @Override
  public boolean isEcsTlrFeatureEnabled() {
    return isEcsTlrFeatureEnabled(tenantService.isCurrentTenantCentral());
  }

  @Override
  public boolean isEcsTlrFeatureEnabled(String tenantId) {
    return isEcsTlrFeatureEnabled(tenantService.isCentralTenant(tenantId));
  }

  @Override
  public boolean isEcsTlrFeatureEnabled(boolean isCentralTenant) {
    return isCentralTenant
      ? isEcsTlrFeatureEnabledInCentralTenant()
      : isTlrEnabledInCirculationSettings();
  }

  private boolean isTlrEnabledInCirculationSettings() {
    log.debug("getCirculationSettings:: Getting circulation settings");
    var circulationSettingsResponse = circulationClient.getCirculationSettingsByQuery(ECS_TLR_FEATURE_SETTINGS);
    if (circulationSettingsResponse.getTotalRecords() > 0) {
      try {
        var circulationSettings = circulationSettingsResponse.getCirculationSettings().get(0);
        log.info("getCirculationSettings:: circulation settings: {}",
          circulationSettings.getValue());
        return circulationSettings.getValue().getEnabled();
      } catch (Exception e) {
        log.error("getCirculationSettings:: Failed to parse circulation settings", e);
      }
    }
    return false;
  }

  private boolean isEcsTlrFeatureEnabledInCentralTenant() {
    Boolean ecsTlrFeatureEnabled = ecsTlrClient.getTlrSettings().getEcsTlrFeatureEnabled();
    log.info("isEcsTlrFeatureEnabled:: {}", ecsTlrFeatureEnabled);
    return ecsTlrFeatureEnabled;
  }
}
