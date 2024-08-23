package org.folio.circulationbff.api;

import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import lombok.SneakyThrows;

public class TenantApiTest extends BaseIT {
  
  public static final String TENANT_URL_POST = "/_/tenant";
  public static final String TENANT_URL_GET_DELETE = format("/_/tenant/%s", TENANT_ID_CONSORTIUM);
  public static final String TENANT_POST_BODY =
    "{\"module_to\": \"mod-circulation-bff-1.0.0-SNAPSHOT\"}";

  @Test
  @SneakyThrows
  void tenantApiPostRespondsWithNoContent() {
    mockMvc.perform(post(TENANT_URL_POST)
        .content(TENANT_POST_BODY)
        .headers(getDefaultTenantApiHeaders())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNoContent());
  }

  @Test
  @SneakyThrows
  void tenantApiGetRespondsWithNoContent() {
    mockMvc.perform(get(TENANT_URL_GET_DELETE)
        .headers(getDefaultTenantApiHeaders())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNoContent());
  }

  @Test
  @SneakyThrows
  void tenantApiDeleteRespondsWithNoContent() {
    mockMvc.perform(delete(TENANT_URL_GET_DELETE)
        .headers(getDefaultTenantApiHeaders())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isNoContent());
  }

  private HttpHeaders getDefaultTenantApiHeaders() {
    final HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.put(XOkapiHeaders.TENANT, List.of(TENANT_ID_CONSORTIUM));
    return httpHeaders;
  }

}
