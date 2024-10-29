package org.folio.circulationbff.service.impl;

import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.spring.service.TenantService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@Primary
public class CustomTenantService extends TenantService {

  private final PrepareSystemUserService prepareSystemUserService;

  public CustomTenantService(FolioExecutionContext context,
    PrepareSystemUserService prepareSystemUserService) {

    super(null, context, null);
    this.prepareSystemUserService = prepareSystemUserService;
  }

  @Override
  protected void afterTenantUpdate(TenantAttributes tenantAttributes) {
    log.info("afterTenantUpdate:: creating system user");
    prepareSystemUserService.setupSystemUser();
  }
}
