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
  public UserTenant getFirstUserTenant() {
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
