package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.UserTenant;

public interface UserTenantsService {
  String getCentralTenant();
  boolean isCentralTenant();
  UserTenant getFirstUserTenant();
  boolean isCentralTenant(String tenantId);
  boolean isCentralTenant(UserTenant userTenant);
}
