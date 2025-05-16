package org.folio.circulationbff.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.circulationbff.client.feign.CheckOutClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.client.feign.RequestMediatedClient;
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
  private final RequestMediatedClient requestMediatedClient;

  @Override
  public CheckOutResponse checkOut(CheckOutRequest request) {
    log.info("checkOut: checking out item with barcode {} from service point {}",
      request.getItemBarcode(), request.getServicePointId());

    if (settingsService.isEcsTlrFeatureEnabled()) {
      if (tenantService.isCurrentTenantCentral()) {
        log.info("checkOut:: doing ECS checkout in central tenant");
        return ecsTlrClient.checkOutByBarcode(request);
      } else if (tenantService.isCurrentTenantSecure()) {
        log.info("checkOut:: doing ECS checkout in secure tenant");
        return requestMediatedClient.checkOutByBarcode(request);
      }
    }
    log.info("checkOut: doing regular checkout in local tenant");
    return checkOutClient.checkOut(request);
  }
}
