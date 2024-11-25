package org.folio.circulationbff.service.impl;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.domain.dto.AllowedServicePointParams;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.BffRequest;
import org.folio.circulationbff.domain.dto.EcsTlr;
import org.folio.circulationbff.domain.dto.PickSlipCollection;
import org.folio.circulationbff.domain.dto.Request;
import org.folio.circulationbff.domain.dto.SearchSlipCollection;
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
  public PickSlipCollection fetchPickSlipsByServicePointId(String servicePointId) {
    log.info("fetchPickSlipsByServicePointId:: servicePointId: {}", servicePointId);
    PickSlipCollection pickSlips = null;
    if(shouldFetchStaffSlipsFromModTlr()) {
      log.info("fetchPickSlipsByServicePointId:: BEFORE TLR CLIENT client; pickSlips: {}", pickSlips);
      pickSlips = ecsTlrClient.getPickSlips(servicePointId);
      log.info("fetchPickSlipsByServicePointId:: AFTER TLR CLIENT client; pickSlips: {}", pickSlips);
    } else {
      log.info("fetchPickSlipsByServicePointId:: BEFORE CIRCULATION CLIENT client; pickSlips: {}", pickSlips);
      pickSlips = circulationClient.getPickSlips(servicePointId);
      log.info("fetchPickSlipsByServicePointId:: AFTER CIRCULATION CLIENT client; pickSlips: {}", pickSlips);
    }
    return pickSlips;
  }

  @Override
  public SearchSlipCollection fetchSearchSlipsByServicePointId(String servicePointId) {
    log.info("fetchSearchSlipsByServicePointId:: servicePointId: {}", servicePointId);
    SearchSlipCollection searchSlips = null;
    if(shouldFetchStaffSlipsFromModTlr()) {
      log.info("fetchSearchSlipsByServicePointId:: BEFORE TLR CLIENT client; searchSlips: {}", searchSlips);
      searchSlips = ecsTlrClient.getSearchSlips(servicePointId);
      log.info("fetchSearchSlipsByServicePointId:: AFTER TLR CLIENT client; searchSlips: {}", searchSlips);
    } else {
      log.info("fetchSearchSlipsByServicePointId:: BEFORE CIRCULATION CLIENT client; searchSlips: {}", searchSlips);
      searchSlips = circulationClient.getSearchSlips(servicePointId);
      log.info("fetchSearchSlipsByServicePointId:: AFTER CIRCULATION CLIENT client; searchSlips: {}", searchSlips);
    }
    return searchSlips;
  }

  @Override
  public AllowedServicePoints getAllowedServicePoints(AllowedServicePointParams params, String tenantId) {
    log.info("getAllowedServicePoints:: params: {}", params);
    if (settingsService.isEcsTlrFeatureEnabled(tenantId) && userTenantsService.isCentralTenant(tenantId)) {
      log.info("getAllowedServicePoints:: Ecs TLR Feature is enabled. Getting allowed service " +
        "points from mod-tlr module");
      return ecsTlrClient.getAllowedServicePoints(params);
    } else {
      log.info("getAllowedServicePoints:: Ecs TLR Feature is disabled. Getting allowed service " +
        "points from mod-circulation module");
      return circulationClient.allowedServicePoints(params);
    }
  }

  @Override
  public Request createRequest(BffRequest request, String tenantId) {
    log.info("createRequest:: request: {}", request.getId());
    if (settingsService.isEcsTlrFeatureEnabled(tenantId) && userTenantsService.isCentralTenant(tenantId)) {
      log.info("createRequest:: ECS TLR Feature is enabled. Creating ECS TLR");
      EcsTlr tlrRequest = ecsTlrClient.createRequest(request);
      return circulationClient.getRequestById(tlrRequest.getPrimaryRequestId());
    } else {
      log.info("createRequest:: Ecs TLR Feature is disabled. Creating circulation request");
      return circulationClient.createRequest(request);
    }
  }

  private boolean shouldFetchStaffSlipsFromModTlr() {
    boolean isCentralTenant = userTenantsService.isCentralTenant();
    boolean ecsTlrFeatureIsEnabledInModTlr = false;
    if (isCentralTenant) {
      ecsTlrFeatureIsEnabledInModTlr = ecsTlrClient.getTlrSettings().getEcsTlrFeatureEnabled();
    }
    log.info("shouldFetchStaffSlipsFromModTlr:: {}", ecsTlrFeatureIsEnabledInModTlr);
    return ecsTlrFeatureIsEnabledInModTlr;
  }
}
