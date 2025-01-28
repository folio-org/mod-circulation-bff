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

    if (!settingsService.isEcsTlrFeatureEnabled()) {
      log.info("getAllowedServicePoints:: Ecs TLR Feature is disabled. " +
        "Calling local mod-circulation.");
      return circulationClient.allowedServicePoints(params);
    }

    if (userTenantsService.isCentralTenant()) {
      log.info("getAllowedServicePoints:: Ecs TLR Feature is enabled and we are in the central " +
        "tenant. Calling local mod-tlr.");
      return ecsTlrClient.getAllowedServicePoints(params);
    }

    log.info("getAllowedServicePoints:: Ecs TLR Feature is enabled and current tenant is not " +
      "central.");

    if (params.getRequestId() == null) {
//      log.info("getAllowedServicePoints:: Request ID is missing (creation).");
//      if (params.getItemId() != null) {
//        log.info("getAllowedServicePoints:: Item ID is present (ILR).");
//        // Trying to fetch item to detect Mediated request creation case
//        Item item = itemStorageClient.findItem(params.getItemId().toString());
//        if (item == null) {
//          log.info("getAllowedServicePoints:: Item not found in the current tenant. " +
//            "Calling central mod-tlr.");
//          return getAllowedSpFromCentralTlr(params);
//        }
//      }
//      log.info("getAllowedServicePoints:: Item ID is either missing (TLR) or present and item " +
//        "found locally. Calling local mod-circulation");
//      return circulationClient.allowedServicePoints(params);

      log.info("getAllowedServicePoints:: Request ID is missing (creation). Calling central mod-tlr.");
      // This should handle both mediated request and local data tenant request cases.
      // In case of a local request, central mod-tlr should call mod-circulation of the current
      // data tenant anyway.
      return getAllowedSpFromCentralTlr(params);
    }

    log.info("getAllowedServicePoints:: Request ID is present (editing).");

    var request = circulationClient.getRequestById(params.getRequestId().toString());
    if (request.getEcsRequestPhase() == null) {
      log.info("getAllowedServicePoints:: Request is not an ECS request. Calling local mod-circulation.");
      return circulationClient.allowedServicePoints(params);
    }

    log.info("getAllowedServicePoints:: Ecs TLR Feature is enabled and request is " +
      "an ECS request. Calling central mod-tlr.");
    return getAllowedSpFromCentralTlr(params);
  }

  private AllowedServicePoints getAllowedSpFromCentralTlr(AllowedServicePointParams params) {
    String patronGroupId = params.getRequesterId() == null ? null :
      userService.find(params.getRequesterId().toString()).getPatronGroup();
    params.setPatronGroupId(patronGroupId != null ? UUID.fromString(patronGroupId) : null);
    params.setRequesterId(null);
    return executionService.executeSystemUserScoped(userTenantsService.getCentralTenant(),
      () -> ecsTlrClient.getAllowedServicePoints(params));
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
