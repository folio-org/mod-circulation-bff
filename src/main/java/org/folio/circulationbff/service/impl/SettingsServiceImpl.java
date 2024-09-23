package org.folio.circulationbff.service.impl;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.service.SettingsService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class SettingsServiceImpl implements SettingsService {

    private final EcsTlrClient ecsTlrClient;
    private final CirculationClient circulationClient;

    @Override
    public boolean isEcsTlrFeatureEnabled() {
      return getCirculationSettings();
    }

    private boolean getTlrSettings() {
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
