package org.folio.circulationbff.service.impl;

import java.util.UUID;

import org.folio.circulationbff.service.SettingsService;
import org.folio.circulationbff.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public abstract class AbstractLoanActionService<R> {

  private final SettingsService settingsService;
  private final TenantService tenantService;

  ResponseEntity<Void> perform(UUID loanId, R request) {
    log.info("perform:: ({}) loanId: {}, request: {}",
      this::getActionName, () -> loanId, () -> toLogString(request));

    String currentTenantId = tenantService.getCurrentTenantId();

    if (!settingsService.isEcsTlrFeatureEnabled(currentTenantId)) {
      log.info("perform:: ECS TLR feature is not enabled for tenant: {}, " +
        "using local service for {}", () -> currentTenantId, this::getActionName);
      return performInCirculation(loanId, request);
    }

    log.info("perform:: ({}) ECS TLR feature is enabled for tenant: {}", this::getActionName,
      () -> currentTenantId);

    if (tenantService.isCentralTenant(currentTenantId)) {
      log.info("perform:: Performing {} in central tenant", this::getActionName);
      return performInTlr(loanId, request);
    }

    if (tenantService.isSecureTenant(currentTenantId)) {
      log.info("perform:: Performing {} in secure tenant", this::getActionName);
      return performInRequestsMediated(loanId, request);
    }

    log.info("perform:: Tenant is neither central nor secure, using local service for {}",
      this::getActionName);
    return performInCirculation(loanId, request);
  }

  abstract ResponseEntity<Void> performInCirculation(UUID loanId, R request);
  abstract ResponseEntity<Void> performInTlr(UUID loanId, R request);
  abstract ResponseEntity<Void> performInRequestsMediated(UUID loanId, R request);
  abstract String getActionName();
  // Not all request fields are safe to log. `comment` is not safe, for example.
  abstract String toLogString(R request);

}

