package org.folio.circulationbff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.Data;

@Configuration
@Data
@ConfigurationProperties("folio.tenant")
public class TenantConfig {
  private String secureTenantId;

  @PostConstruct
  private void postConstruct() {
    if ("${SECURE_TENANT_ID}".equals(secureTenantId)) {
      secureTenantId = null;
    }
  }
}
