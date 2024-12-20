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
import org.folio.circulationbff.service.UserService;
import org.folio.circulationbff.service.UserTenantsService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class CirculationBffServiceImpl implements CirculationBffService {

  private final CirculationClient circulationClient;
  private final EcsTlrClient ecsTlrClient;
  private final UserService userService;
  private final SettingsService settingsService;
  private final UserTenantsService userTenantsService;
  private final SystemUserScopedExecutionService executionService;

  @Override
  public PickSlipCollection fetchPickSlipsByServicePointId(String servicePointId) {
    log.info("fetchPickSlipsByServicePointId:: servicePointId: {}", servicePointId);
    return shouldFetchStaffSlipsFromTlr()
      ? ecsTlrClient.getPickSlips(servicePointId)
      : circulationClient.getPickSlips(servicePointId);
  }

  @Override
  public SearchSlipCollection fetchSearchSlipsByServicePointId(String servicePointId) {
    log.info("fetchSearchSlipsByServicePointId:: servicePointId: {}", servicePointId);
    return shouldFetchStaffSlipsFromTlr()
      ? ecsTlrClient.getSearchSlips(servicePointId)
      : circulationClient.getSearchSlips(servicePointId);
  }

  @Override
  public AllowedServicePoints getAllowedServicePoints(AllowedServicePointParams params, String tenantId) {
    log.info("getAllowedServicePoints:: params: {}", params);
    if (settingsService.isEcsTlrFeatureEnabled()) {
      if (userTenantsService.isCentralTenant()) {
        log.info("getAllowedServicePoints:: Ecs TLR Feature is enabled. " +
          "Getting allowed service points from local mod-tlr");
        return ecsTlrClient.getAllowedServicePoints(params);
      } else {
        log.info("getAllowedServicePoints:: Ecs TLR Feature is enabled. " +
          "Getting allowed service points from central mod-tlr");
        String patronGroupId = userService.find(params.getRequesterId().toString()).getPatronGroup();
        params.setPatronGroupId(UUID.fromString(patronGroupId));
        params.setRequesterId(null);
        return executionService.executeSystemUserScoped(userTenantsService.getCentralTenant(),
          () -> ecsTlrClient.getAllowedServicePoints(params));
      }
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

  private boolean shouldFetchStaffSlipsFromTlr() {
    boolean isCentralTenant = userTenantsService.isCentralTenant();
    boolean ecsTlrFeatureIsEnabledInTlr = false;
    if (isCentralTenant) {
      ecsTlrFeatureIsEnabledInTlr = ecsTlrClient.getTlrSettings().getEcsTlrFeatureEnabled();
    }
    log.info("shouldFetchStaffSlipsFromTlr:: {}", ecsTlrFeatureIsEnabledInTlr);
    return ecsTlrFeatureIsEnabledInTlr;
  }
}
