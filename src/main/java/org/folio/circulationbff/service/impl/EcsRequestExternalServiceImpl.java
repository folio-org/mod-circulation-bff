package org.folio.circulationbff.service.impl;

import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.domain.dto.EcsRequestExternal;
import org.folio.circulationbff.domain.dto.EcsTlr;
import org.folio.circulationbff.service.EcsRequestExternalService;
import org.folio.circulationbff.service.UserTenantsService;
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
  private final UserTenantsService userTenantsService;


  @Override
  public EcsTlr createEcsRequestExternal(EcsRequestExternal ecsRequestExternal) {
    String centralTenantId = userTenantsService.getCentralTenant();

    return systemUserScopedExecutionService.executeSystemUserScoped(centralTenantId,
      () -> ecsTlrClient.createEcsExternalRequest(ecsRequestExternal));
  }
}
