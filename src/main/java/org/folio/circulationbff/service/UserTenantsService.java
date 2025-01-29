package org.folio.circulationbff.service;

public interface UserTenantsService {
  String getCurrentTenant();
  String getCentralTenant();
  boolean isCentralTenant();
  boolean isCentralTenant(String tenantId);
}
