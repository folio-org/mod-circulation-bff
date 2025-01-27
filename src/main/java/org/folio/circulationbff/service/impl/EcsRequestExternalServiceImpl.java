package org.folio.circulationbff.service.impl;

import static org.folio.circulationbff.domain.dto.EcsRequestExternal.RequestLevelEnum.ITEM;
import static org.folio.circulationbff.domain.dto.EcsRequestExternal.RequestLevelEnum.TITLE;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.domain.EcsTenantConfiguration;
import org.folio.circulationbff.domain.dto.ConsortiumItem;
import org.folio.circulationbff.domain.dto.EcsRequestExternal;
import org.folio.circulationbff.domain.dto.EcsRequestExternal.RequestLevelEnum;
import org.folio.circulationbff.domain.dto.EcsTlr;
import org.folio.circulationbff.domain.dto.Request;
import org.folio.circulationbff.service.EcsTenantConfigurationService;
import org.folio.circulationbff.service.EcsRequestExternalService;
import org.folio.circulationbff.service.SearchService;
import org.folio.circulationbff.service.SettingsService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class EcsRequestExternalServiceImpl implements EcsRequestExternalService {

  private final SystemUserScopedExecutionService systemUserScopedExecutionService;
  private final EcsTlrClient ecsTlrClient;
  private final CirculationClient circulationClient;
  private final SettingsService settingsService;
  private final SearchService searchService;
  private final EcsTenantConfigurationService ecsTenantConfigurationService;

  @Override
  public Request createEcsRequestExternal(EcsRequestExternal request) {
    log.info("createEcsRequestExternal:: creating external request");
    fetchMissingRequestProperties(request);
    EcsTenantConfiguration tenantConfiguration = ecsTenantConfigurationService.getTenantConfiguration();

    return settingsService.isEcsTlrFeatureEnabled(tenantConfiguration.isCurrentTenantCentral())
      ? createEcsRequest(request, tenantConfiguration)
      : createCirculationRequest(request);
  }

  private Request createEcsRequest(EcsRequestExternal ecsRequestExternal,
    EcsTenantConfiguration tenantConfiguration) {

    log.info("createEcsRequest:: creating ECS request");
    return tenantConfiguration.isCurrentTenantSecure()
      ? createMediatedRequest(ecsRequestExternal)
      : createExternalEcsTlr(ecsRequestExternal, tenantConfiguration);
  }

  private Request createCirculationRequest(EcsRequestExternal ecsRequestExternal) {
    log.info("createCirculationRequest:: creating circulation request");
    return ecsRequestExternal.getRequestLevel() == TITLE
      ? createTitleLevelRequest(ecsRequestExternal)
      : createItemLevelRequest(ecsRequestExternal);
  }

  private Request createExternalEcsTlr(EcsRequestExternal ecsRequestExternal,
    EcsTenantConfiguration tenantConfiguration) {

    log.info("createExternalEcsTlr:: creating ECS TLR");
    EcsTlr ecsTlr = systemUserScopedExecutionService.executeSystemUserScoped(
      tenantConfiguration.centralTenantId(),
      () -> ecsTlrClient.createEcsExternalRequest(ecsRequestExternal));
    log.info("createExternalEcsTlr:: ECS TLR created: {}", ecsTlr::getId);
    log.debug("createExternalEcsTlr:: ecsTlr: {}", ecsTlr);

    log.info("createExternalEcsTlr:: fetching primary request");
    return circulationClient.getRequestById(ecsTlr.getPrimaryRequestId());
  }

  private Request createMediatedRequest(EcsRequestExternal ecsRequestExternal) {
    log.info("createMediatedRequest:: creating mediated request");
    // POST /requests-mediated/mediated-requests
    return new Request();
  }

  private Request createItemLevelRequest(EcsRequestExternal ecsRequestExternal) {
    log.info("createItemLevelRequest:: creating item level request");
    // POST /circulation/requests
    return new Request();
  }

  private Request createTitleLevelRequest(EcsRequestExternal ecsRequestExternal) {
    log.info("createTitleLevelRequest:: creating title level request");
    // POST /circulation/requests/instances
    return new Request();
  }

  private void fetchMissingRequestProperties(EcsRequestExternal request) {
    String itemId = request.getItemId();
    RequestLevelEnum requestLevel = request.getRequestLevel();
    log.info("fetchMissingRequestProperties:: requestLevel={}, itemId={}", requestLevel, itemId);
    if (requestLevel == ITEM && itemId != null) {
      log.info("fetchMissingRequestProperties:: fetching item for item level request");
      ConsortiumItem item = searchService.findConsortiumItem(itemId);
      request.instanceId(item.getInstanceId())
        .holdingsRecordId(item.getHoldingsRecordId());
    }
  }

}
