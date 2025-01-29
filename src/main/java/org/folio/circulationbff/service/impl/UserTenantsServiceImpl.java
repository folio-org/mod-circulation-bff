package org.folio.circulationbff.service.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.circulationbff.client.feign.UserTenantsClient;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.folio.circulationbff.domain.dto.UserTenantCollection;
import org.folio.circulationbff.service.UserTenantsService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class UserTenantsServiceImpl implements UserTenantsService {

  private final UserTenantsClient userTenantsClient;

  @Override
  public String getCurrentTenant() {
    UserTenant firstUserTenant = getFirstUserTenant();
    if (firstUserTenant == null) {
      return null;
    }
    String currentTenantId = firstUserTenant.getTenantId();
    log.info("getCurrentTenant:: currentTenantId={}", currentTenantId);
    return currentTenantId;
  }

  @Override
  public String getCentralTenant() {
    UserTenant firstUserTenant = getFirstUserTenant();
    if (firstUserTenant == null) {
      return null;
    }
    String centralTenantId = firstUserTenant.getCentralTenantId();
    log.info("getCentralTenant:: centralTenantId={}", centralTenantId);
    return centralTenantId;
  }

  @Override
  public boolean isCentralTenant() {
    UserTenant firstUserTenant = getFirstUserTenant();
    if (firstUserTenant == null) {
      return false;
    }
    String centralTenantId = firstUserTenant.getCentralTenantId();
    String tenantId = firstUserTenant.getTenantId();
    log.info("isCentralTenant:: centralTenantId={}, tenantId={}", centralTenantId,
      tenantId);

    return centralTenantId.equals(tenantId);
  }

  @Override
  public boolean isCentralTenant(String tenantId) {
    UserTenant firstUserTenant = getFirstUserTenant();
    if (firstUserTenant == null) {
      return false;
    }
    String centralTenantId = firstUserTenant.getCentralTenantId();
    if (centralTenantId != null && centralTenantId.equals(tenantId)) {
      log.info("isCentralTenant: tenantId={} is central tenant", tenantId);
      return true;
    }
    return false;
  }

  private UserTenant getFirstUserTenant() {
    log.info("getFirstUserTenant:: finding first userTenant");
    UserTenantCollection userTenants = userTenantsClient.getUserTenants(1);
    log.info("getFirstUserTenant:: userTenants: {}", () -> userTenants);
    if (userTenants == null || CollectionUtils.isEmpty(userTenants.getUserTenants())) {
      log.warn("getFirstUserTenant: failed to fetch user tenants");
      return null;
    }
    var firstUserTenant = userTenants.getUserTenants().get(0);
    log.info("getFirstUserTenant:: result: {}", firstUserTenant);
    return firstUserTenant;
  }
}
