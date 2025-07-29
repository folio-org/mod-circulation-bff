package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    var loanId = randomId();
    var userTenant = new UserTenant(randomId(), TENANT_ID_COLLEGE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockHelper.mockUserTenants(userTenant, TENANT_ID_COLLEGE);
    mockHelper.mockEcsTlrCirculationSettings(false, TENANT_ID_COLLEGE);

    wireMockServer.stubFor(WireMock.post(urlPathEqualTo(format(CIRCULATION_DECLARE_ITEM_LOST_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(WireMock.noContent()));

    performDeclareItemLost(TENANT_ID_COLLEGE, loanId, new DeclareItemLostRequest()
      .declaredLostDateTime(new Date())
      .servicePointId(UUID.randomUUID()));

    wireMockServer.verify(postRequestedFor(urlPathEqualTo(format(CIRCULATION_DECLARE_ITEM_LOST_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE)));
  }

  @Test
  @SneakyThrows
  void callsTlrWhenEcsTlrEnabledInCentralTenant() {
    var loanId = randomId();
    var userTenant = new UserTenant(randomId(), TENANT_ID_CONSORTIUM);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockHelper.mockUserTenants(userTenant, TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);

    wireMockServer.stubFor(WireMock.post(urlPathEqualTo(format(TLR_DECLARE_ITEM_LOST_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(noContent()));

    performDeclareItemLost(TENANT_ID_CONSORTIUM, loanId, new DeclareItemLostRequest()
      .declaredLostDateTime(new Date())
      .servicePointId(UUID.randomUUID()));

    wireMockServer.verify(postRequestedFor(urlPathEqualTo(format(TLR_DECLARE_ITEM_LOST_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));
  }

  @Test
  @SneakyThrows
  void callsRequestMediatedWhenEcsTlrEnabledAndCurrentTenantSecure() {
    var loanId = randomId();
    var userTenant = new UserTenant(randomId(), TENANT_ID_SECURE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockHelper.mockUserTenants(userTenant, TENANT_ID_SECURE);
    mockHelper.mockEcsTlrCirculationSettings(true, TENANT_ID_SECURE);

    wireMockServer.stubFor(WireMock.post(urlPathEqualTo(format(REQUESTS_MEDIATED_DECLARE_ITEM_LOST_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_SECURE))
      .willReturn(noContent()));

    performDeclareItemLost(TENANT_ID_SECURE, loanId, new DeclareItemLostRequest()
      .declaredLostDateTime(new Date())
      .servicePointId(UUID.randomUUID()));

    wireMockServer.verify(postRequestedFor(urlPathEqualTo(format(REQUESTS_MEDIATED_DECLARE_ITEM_LOST_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_SECURE)));
  }

  @Test
  @SneakyThrows
  void fallbackToCirculationWhenEcsTlrEnabledButNotCentralOrSecure() {
    var loanId = randomId();
    var userTenant = new UserTenant(randomId(), TENANT_ID_COLLEGE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockHelper.mockUserTenants(userTenant, TENANT_ID_COLLEGE);
    mockHelper.mockEcsTlrCirculationSettings(true, TENANT_ID_COLLEGE);

    wireMockServer.stubFor(WireMock.post(urlPathEqualTo(format(CIRCULATION_DECLARE_ITEM_LOST_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(noContent()));

    performDeclareItemLost(TENANT_ID_COLLEGE, loanId, new DeclareItemLostRequest()
      .declaredLostDateTime(new Date())
      .servicePointId(UUID.randomUUID()));

    wireMockServer.verify(postRequestedFor(urlPathEqualTo(format(CIRCULATION_DECLARE_ITEM_LOST_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE)));
  }

  private void performDeclareItemLost(String tenantId, String loanId, DeclareItemLostRequest request) throws Exception {
    mockMvc.perform(post(format(DECLARE_ITEM_LOST_URL, loanId))
        .headers(buildHeaders(tenantId))
        .contentType(APPLICATION_JSON)
        .content(asJsonString(request)))
      .andExpect(status().isNoContent());
  }
}
