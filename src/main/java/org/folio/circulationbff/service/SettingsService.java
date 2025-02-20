package org.folio.circulationbff.service;

public interface SettingsService {
  boolean isEcsTlrFeatureEnabled();
  boolean isEcsTlrFeatureEnabled(String tenantId);
}
