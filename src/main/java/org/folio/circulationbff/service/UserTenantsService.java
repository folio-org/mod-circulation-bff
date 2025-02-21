package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.UserTenant;

public interface UserTenantsService {
  String getCurrentTenant();
  String getCentralTenant();
  boolean isCentralTenant();
  boolean isCentralTenant(String tenantId);
  UserTenant getFirstUserTenant();
}
