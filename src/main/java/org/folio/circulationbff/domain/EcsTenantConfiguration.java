package org.folio.circulationbff.domain;

public record EcsTenantConfiguration(boolean isConsortiaEnabled, String currentTenantId,
  String centralTenantId, String secureTenantId) {

  public boolean isCurrentTenantCentral() {
    return isConsortiaEnabled && currentTenantId != null && currentTenantId.equals(centralTenantId);
  }

  public boolean isCurrentTenantSecure() {
    return isConsortiaEnabled && currentTenantId != null && currentTenantId.equals(secureTenantId);
  }
}
