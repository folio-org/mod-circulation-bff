package org.folio.circulationbff.service.impl;

import static org.folio.circulationbff.domain.dto.EcsRequestExternal.RequestLevelEnum.ITEM;
import static org.folio.circulationbff.domain.dto.EcsRequestExternal.RequestLevelEnum.TITLE;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.domain.dto.ConsortiumItem;
import org.folio.circulationbff.domain.dto.EcsRequestExternal;
import org.folio.circulationbff.domain.dto.EcsRequestExternal.RequestLevelEnum;
import org.folio.circulationbff.domain.dto.EcsTlr;
import org.folio.circulationbff.domain.dto.Request;
import org.folio.circulationbff.service.EcsRequestExternalService;
import org.folio.circulationbff.service.SearchService;
import org.folio.circulationbff.service.SettingsService;
import org.folio.circulationbff.service.TenantService;
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
  private final TenantService tenantService;

  @Override
  public Request createEcsRequestExternal(EcsRequestExternal request) {
    log.info("createEcsRequestExternal:: requesterId={}, itemId={}, instanceId={}",
      request::getRequesterId, request::getItemId, request::getInstanceId);

    fetchMissingRequestProperties(request);

    return settingsService.isEcsTlrFeatureEnabled()
      ? createEcsRequest(request)
      : createCirculationRequest(request);
  }

  private Request createEcsRequest(EcsRequestExternal ecsRequestExternal) {
    log.info("createEcsRequest:: creating ECS request");
    return tenantService.isCurrentTenantSecure()
      ? createMediatedRequest(ecsRequestExternal)
      : createExternalEcsTlr(ecsRequestExternal);
  }

  private Request createCirculationRequest(EcsRequestExternal ecsRequestExternal) {
    log.info("createCirculationRequest:: creating circulation request");
    return ecsRequestExternal.getRequestLevel() == TITLE
      ? createTitleLevelRequest(ecsRequestExternal)
      : createItemLevelRequest(ecsRequestExternal);
  }

  private Request createExternalEcsTlr(EcsRequestExternal ecsRequestExternal) {
    log.info("createExternalEcsTlr:: creating ECS TLR");
    EcsTlr ecsTlr = systemUserScopedExecutionService.executeSystemUserScoped(
      tenantService.getCentralTenantId().orElseThrow(),
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
