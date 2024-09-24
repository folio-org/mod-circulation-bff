package org.folio.circulationbff.service.impl;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.service.SettingsService;
import org.folio.circulationbff.service.UserTenantsService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class SettingsServiceImpl implements SettingsService {

    private final EcsTlrClient ecsTlrClient;
    private final CirculationClient circulationClient;
    private final UserTenantsService userTenantsService;

    @Override
    public boolean isEcsTlrFeatureEnabled(String tenantId) {
      if (userTenantsService.isCentralTenant(tenantId)) {
        return getTlrSettings();
      }
      return getCirculationSettings();
    }

    private boolean getTlrSettings() {
      log.info("getTlrSettings:: Getting TLR settings");
      return ecsTlrClient.getTlrSettings().getEcsTlrFeatureEnabled();
    }

    private boolean getCirculationSettings() {
      log.info("getCirculationSettings:: Getting circulation settings");
      var circulationSettingsResponse = circulationClient.getEcsTlrCirculationSettings();
      if (circulationSettingsResponse.getTotalRecords() > 0) {
        try {
          var circulationSettings = circulationSettingsResponse.getCirculationSettings().get(0);
          return circulationSettings.getValue().getEnabled();
        } catch (Exception e) {
          log.error("getCirculationSettings:: Failed to parse circulation settings", e);
        }
      }
      return false;
    }
}
