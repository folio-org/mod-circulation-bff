package org.folio.circulationbff.service.impl;

import org.folio.circulationbff.config.TenantConfig;
import org.folio.circulationbff.domain.EcsTenantConfiguration;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.folio.circulationbff.service.EcsTenantConfigurationService;
import org.folio.circulationbff.service.UserTenantsService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class EcsTenantConfigurationServiceImpl
  implements EcsTenantConfigurationService {

  private final UserTenantsService userTenantsService;
  private final TenantConfig tenantConfig;

  @Override
  public EcsTenantConfiguration getTenantConfiguration() {
    UserTenant userTenant = userTenantsService.getFirstUserTenant();

    EcsTenantConfiguration tenantConfiguration = userTenant == null
      ? new EcsTenantConfiguration(false, null, null, null)
      : new EcsTenantConfiguration(true, userTenant.getTenantId(), userTenant.getCentralTenantId(),
        tenantConfig.getSecureTenantId());

    log.info("getTenantConfiguration:: {}", tenantConfiguration);
    return tenantConfiguration;
  }
}
