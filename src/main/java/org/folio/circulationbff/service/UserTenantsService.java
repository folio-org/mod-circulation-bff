package org.folio.circulationbff.service;

public interface UserTenantsService {
  boolean isCentralTenant();
  boolean isCentralTenant(String tenantId);
  String getCentralTenantId();
}
