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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class CheckOutServiceImpl implements CheckOutService {

  private final SettingsService settingsService;
  private final TenantService tenantService;
  private final CheckOutClient checkOutClient;
  private final EcsTlrClient ecsTlrClient;

  @Override
  public CheckOutResponse checkOut(CheckOutRequest request) {
    log.info("checkOut: checking out item with barcode {} from service point {}",
      request.getItemBarcode(), request.getServicePointId());
    if (settingsService.isEcsTlrFeatureEnabled() && tenantService.isCurrentTenantCentral()) {
      log.info("Check out by barcode in mod-tlr module");
      return ecsTlrClient.checkOutByBarcode(request);
    }
    log.info("Check out by barcode in mod-circulation module");
    return checkOutClient.checkOut(request);
  }
}
