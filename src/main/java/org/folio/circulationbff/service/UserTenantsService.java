package org.folio.circulationbff.service;

public interface UserTenantsService {
  String getCentralTenant();
  boolean isCentralTenant();
  boolean isCentralTenant(String tenantId);
  String getCentralTenantId();
}
