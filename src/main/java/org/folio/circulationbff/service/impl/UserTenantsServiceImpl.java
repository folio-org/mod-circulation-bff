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
  public boolean isCentralTenant(String tenantId) {
    UserTenant firstUserTenant = findFirstUserTenant();
    if (firstUserTenant == null) {
      log.info("processUserGroupEvent: Failed to get user-tenants info");
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

