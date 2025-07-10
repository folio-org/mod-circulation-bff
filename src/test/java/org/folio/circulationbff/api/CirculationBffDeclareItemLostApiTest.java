package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static wiremock.org.apache.hc.core5.http.HttpStatus.SC_NO_CONTENT;

import java.util.Date;
import java.util.UUID;

import org.folio.circulationbff.domain.dto.DeclareItemLostRequest;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.folio.circulationbff.service.TenantService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.github.tomakehurst.wiremock.client.WireMock;

import lombok.SneakyThrows;

class CirculationBffDeclareItemLostApiTest extends BaseIT {

  private static final String DECLARE_ITEM_LOST_URL = "/circulation-bff/loans/%s/declare-item-lost";
  private static final String CIRCULATION_DECLARE_ITEM_LOST_URL = "/circulation/loans/%s/declare-item-lost";
  private static final String TLR_DECLARE_ITEM_LOST_URL = "/tlr/loans/declare-item-lost";
  private static final String REQUESTS_MEDIATED_DECLARE_ITEM_LOST_URL = "/requests-mediated/loans/%s/declare-item-lost";
  @Mock
  private TenantService tenantService;

  @Test
  @SneakyThrows
  void callsCirculationWhenEcsTlrDisabled() {
    var loanId = UUID.randomUUID().toString();
    var userTenant = new UserTenant(UUID.randomUUID().toString(), TENANT_ID_COLLEGE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockHelper.mockUserTenants(userTenant, TENANT_ID_COLLEGE);
    mockHelper.mockEcsTlrCirculationSettings(false, TENANT_ID_COLLEGE);

    wireMockServer.stubFor(WireMock.post(urlPathEqualTo(String.format(CIRCULATION_DECLARE_ITEM_LOST_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(WireMock.noContent()));

    performDeclareItemLost(TENANT_ID_COLLEGE, loanId, new DeclareItemLostRequest()
      .declaredLostDateTime(new Date())
      .servicePointId(UUID.randomUUID()));

    wireMockServer.verify(postRequestedFor(urlPathEqualTo(String.format(CIRCULATION_DECLARE_ITEM_LOST_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE)));
  }

  @Test
  @SneakyThrows
  void callsTlrWhenEcsTlrEnabledInCentralTenant() {
    var loanId = UUID.randomUUID().toString();
    var userTenant = new UserTenant(UUID.randomUUID().toString(), TENANT_ID_CONSORTIUM);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockHelper.mockUserTenants(userTenant, TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);

    wireMockServer.stubFor(WireMock.post(urlPathEqualTo(String.format(TLR_DECLARE_ITEM_LOST_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse("", SC_NO_CONTENT)));

    performDeclareItemLost(TENANT_ID_CONSORTIUM, loanId, new DeclareItemLostRequest()
      .declaredLostDateTime(new Date())
      .servicePointId(UUID.randomUUID()));

    wireMockServer.verify(postRequestedFor(urlPathEqualTo(String.format(TLR_DECLARE_ITEM_LOST_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));
  }

  @Test
  @SneakyThrows
  void callsRequestMediatedWhenEcsTlrEnabledAndCurrentTenantSecure() {
    var loanId = UUID.randomUUID().toString();
    var userTenant = new UserTenant(UUID.randomUUID().toString(), TENANT_ID_SECURE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockHelper.mockUserTenants(userTenant, TENANT_ID_SECURE);
    mockHelper.mockEcsTlrCirculationSettings(true, TENANT_ID_SECURE);
    when(tenantService.isCurrentTenantSecure()).thenReturn(true);

    wireMockServer.stubFor(WireMock.post(urlPathEqualTo(String.format(REQUESTS_MEDIATED_DECLARE_ITEM_LOST_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_SECURE))
      .willReturn(jsonResponse("", SC_NO_CONTENT)));

    performDeclareItemLost(TENANT_ID_SECURE, loanId, new DeclareItemLostRequest()
      .declaredLostDateTime(new Date())
      .servicePointId(UUID.randomUUID()));

    wireMockServer.verify(postRequestedFor(urlPathEqualTo(String.format(REQUESTS_MEDIATED_DECLARE_ITEM_LOST_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_SECURE)));
  }

  @Test
  @SneakyThrows
  void fallbackToCirculationWhenEcsTlrEnabledButNotCentralOrSecure() {
    var loanId = UUID.randomUUID().toString();
    var userTenant = new UserTenant(UUID.randomUUID().toString(), TENANT_ID_COLLEGE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockHelper.mockUserTenants(userTenant, TENANT_ID_COLLEGE);
    mockHelper.mockEcsTlrCirculationSettings(true, TENANT_ID_COLLEGE);
    when(tenantService.isCurrentTenantSecure()).thenReturn(false);

    wireMockServer.stubFor(WireMock.post(urlPathEqualTo(String.format(CIRCULATION_DECLARE_ITEM_LOST_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse("", SC_NO_CONTENT)));

    performDeclareItemLost(TENANT_ID_COLLEGE, loanId, new DeclareItemLostRequest()
      .declaredLostDateTime(new Date())
      .servicePointId(UUID.randomUUID()));

    wireMockServer.verify(postRequestedFor(urlPathEqualTo(String.format(CIRCULATION_DECLARE_ITEM_LOST_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE)));
  }

  private void performDeclareItemLost(String tenantId, String loanId, DeclareItemLostRequest request) throws Exception {
    mockMvc.perform(post(String.format(DECLARE_ITEM_LOST_URL, loanId))
        .headers(buildHeaders(tenantId))
        .contentType(APPLICATION_JSON)
        .content(asJsonString(request)))
      .andExpect(status().isNoContent());
  }
}
