package org.folio.circulationbff.service.impl;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.folio.circulationbff.config.TenantConfig;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.folio.circulationbff.service.TenantService;
import org.folio.circulationbff.service.UserTenantsService;
import org.folio.spring.FolioExecutionContext;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class TenantServiceImpl implements TenantService {

  private static final Map<String, Optional<String>> CENTRAL_TENANT_ID_CACHE = new ConcurrentHashMap<>();

  private final UserTenantsService userTenantsService;
  private final TenantConfig tenantConfig;
  private final FolioExecutionContext folioContext;

  @Override
  public String getCurrentTenantId() {
    return folioContext.getTenantId();
  }

  @Override
  public Optional<String> getCentralTenantId() {
    String currentTenantId = getCurrentTenantId();
    Optional<String> centralTenantId;

    if (CENTRAL_TENANT_ID_CACHE.containsKey(currentTenantId)) {
      centralTenantId = CENTRAL_TENANT_ID_CACHE.get(currentTenantId);
      log.info("getCentralTenantId:: cache hit: tenantId={}, centralTenantId={}",
        () -> currentTenantId, () -> centralTenantId.orElse(null));
    } else {
      log.info("getCentralTenantId:: cache miss: tenantId={}", currentTenantId);
      centralTenantId = Optional.ofNullable(userTenantsService.getFirstUserTenant())
          .map(UserTenant::getCentralTenantId);
      log.info("getCentralTenantId:: populating cache: tenantId={}, centralTenantId={}",
        () -> currentTenantId, () -> centralTenantId.orElse(null));
      CENTRAL_TENANT_ID_CACHE.put(currentTenantId, centralTenantId);
    }
    log.debug("getCentralTenantId:: cache: {}", CENTRAL_TENANT_ID_CACHE);

    return centralTenantId;
  }

  @Override
  public Optional<String> getSecureTenantId() {
    return Optional.ofNullable(tenantConfig.getSecureTenantId());
  }

  @Override
  public boolean isCurrentTenantCentral() {
    return getCentralTenantId()
      .map(getCurrentTenantId()::equals)
      .orElse(false);
  }

  @Override
  public boolean isCurrentTenantSecure() {
    return getSecureTenantId()
      .map(getCurrentTenantId()::equals)
      .orElse(false);
  }

  @Override
  public boolean isCentralTenant(String tenantId) {
    return getCentralTenantId()
      .map(tenantId::equals)
      .orElse(false);
  }

  @Override
  public boolean isSecureTenant(String tenantId) {
    return getSecureTenantId()
      .map(tenantId::equals)
      .orElse(false);
  }

  public static void clearCentralTenantIdCache() {
    log.info("clearCentralTenantIdCache:: clearing cache");
    CENTRAL_TENANT_ID_CACHE.clear();
  }

}
