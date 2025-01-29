package org.folio.circulationbff.service;

import java.util.Optional;

public interface TenantService {
  String getCurrentTenantId();
  Optional<String> getCentralTenantId();
  Optional<String> getSecureTenantId();
  boolean isCurrentTenantCentral();
  boolean isCurrentTenantSecure();
  boolean isCentralTenant(String tenantId);
}
