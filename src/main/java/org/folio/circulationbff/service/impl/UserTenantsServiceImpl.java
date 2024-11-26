package org.folio.circulationbff.service.impl;

import java.util.List;

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
  public String getCentralTenant() {
    UserTenant firstUserTenant = getFirstUserTenant();
    if (firstUserTenant == null) {
      log.info("getCentralTenant:: failed to fetch user tenants");
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
      log.info("isCentralTenant:: failed to fetch user tenants");
      return false;
    }
    String centralTenantId = firstUserTenant.getCentralTenantId();
    String tenantId = firstUserTenant.getTenantId();
    log.info("isCentralTenant:: centralTenantId={}, tenantId={}", centralTenantId,
      tenantId);

    return centralTenantId.equals(tenantId);
  }

  private UserTenant getFirstUserTenant() {
    UserTenant firstUserTenant = findFirstUserTenant();
    if (firstUserTenant == null) {
      log.info("processUserGroupEvent: Failed to get user-tenants info");
    }
    return firstUserTenant;
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

  private UserTenant findFirstUserTenant() {
    log.info("findFirstUserTenant:: finding first userTenant");
    UserTenant firstUserTenant = null;
    UserTenantCollection userTenantCollection = userTenantsClient.getUserTenants(1);
    log.info("findFirstUserTenant:: userTenantCollection: {}", () -> userTenantCollection);
    if (userTenantCollection != null) {
      log.info("findFirstUserTenant:: userTenantCollection: {}", () -> userTenantCollection);
      List<UserTenant> userTenants = userTenantCollection.getUserTenants();
      if (!userTenants.isEmpty()) {
        firstUserTenant = userTenants.get(0);
        log.info("findFirstUserTenant:: found userTenant: {}", firstUserTenant);
      }
    }
    log.info("findFirstUserTenant:: result: {}", firstUserTenant);
    return firstUserTenant;
  }
}

