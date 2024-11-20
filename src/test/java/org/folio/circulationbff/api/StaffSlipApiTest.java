package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.apache.http.HttpStatus.SC_OK;
import static org.folio.circulationbff.api.StaffSlipsApiTestDataProvider.SERVICE_POINT_ID;
import static org.folio.circulationbff.api.StaffSlipsApiTestDataProvider.buildCirculationTlrSettingsResponse;
import static org.folio.circulationbff.api.StaffSlipsApiTestDataProvider.buildStaffSlipCollection;
import static org.folio.circulationbff.api.StaffSlipsApiTestDataProvider.buildUserTenantCollection;
import static org.folio.circulationbff.api.StaffSlipsApiTestDataProvider.buildTlrSettings;
import static org.folio.circulationbff.api.StaffSlipsApiTestDataProvider.isCentralTenantToIsTlrEnabledToUrlForStaffSLipsToCircBffUrl;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import org.apache.http.HttpStatus;
import org.folio.circulationbff.domain.dto.CirculationSettingsResponse;
import org.folio.circulationbff.domain.dto.StaffSlipCollection;
import org.folio.circulationbff.domain.dto.TlrSettings;
import org.folio.circulationbff.domain.dto.UserTenantCollection;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import lombok.SneakyThrows;

class StaffSlipApiTest extends BaseIT {

  private static final String URL_PATTERN = "%s/%s";

  private static Stream<Arguments> testData() {
    return isCentralTenantToIsTlrEnabledToUrlForStaffSLipsToCircBffUrl();
  }

  @ParameterizedTest()
  @MethodSource("testData")
  @SneakyThrows
  void getStaffSlipsApiTest(boolean isCentralTenant, boolean isTlrEnabled, String externalModuleUrl,
    String circulationBffUrl) {

    var tenantId = isCentralTenant ? TENANT_ID_CONSORTIUM : TENANT_ID_COLLEGE;
    StaffSlipCollection staffSlips = buildStaffSlipCollection();

    UrlPathPattern externalModuleUrlPattern = urlPathMatching(String.format(URL_PATTERN,
      externalModuleUrl, SERVICE_POINT_ID));

    mockUserTenants(buildUserTenantCollection(tenantId), tenantId);
    mockTleSettings(isCentralTenant, isTlrEnabled, tenantId);
    mockStaffSlips(staffSlips, externalModuleUrlPattern, tenantId);
    mockPerform(circulationBffUrl, staffSlips, tenantId);

    wireMockServer.verify(1, getRequestedFor(externalModuleUrlPattern));
  }

  @SneakyThrows
  private void mockPerform(String circulationBffUrl, StaffSlipCollection staffSlips,
    String tenantId) {

    HttpHeaders httpHeaders = defaultHeaders();
    httpHeaders.set(XOkapiHeaders.TENANT, tenantId);
    mockMvc.perform(get(circulationBffUrl, SERVICE_POINT_ID)
        .headers(httpHeaders)
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(Json.write(staffSlips)));
  }

  private void mockTleSettings(boolean isCentralTenant, boolean isTlrEnabled, String tenantId) {
    if (isCentralTenant) {
      mockEcsTlrSettings(buildTlrSettings(isTlrEnabled), tenantId);
    } else {
      mockEcsTlrCirculationSettings(buildCirculationTlrSettingsResponse(isTlrEnabled), tenantId);
    }
  }

  private static void mockStaffSlips(StaffSlipCollection staffSlips, UrlPathPattern externalUrl,
    String requesterTenantId) {
    wireMockServer.stubFor(WireMock.get(externalUrl)
      .withHeader(HEADER_TENANT, equalTo(requesterTenantId))
      .willReturn(jsonResponse(staffSlips, HttpStatus.SC_OK)));
  }

  private void mockUserTenants(UserTenantCollection userTenants, String requesterTenantId) {
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(USER_TENANTS_URL))
      .withHeader(HEADER_TENANT, equalTo(requesterTenantId))
      .withQueryParam("limit", matching("\\d*"))
      .willReturn(jsonResponse(asJsonString(userTenants), SC_OK)));
  }

  private void mockEcsTlrCirculationSettings(CirculationSettingsResponse response,
    String requesterTenantId) {

    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(CIRCULATION_SETTINGS_URL))
      .withHeader(HEADER_TENANT, equalTo(requesterTenantId))
      .withQueryParam("query", equalTo("name=ecsTlrFeature"))
      .willReturn(jsonResponse(asJsonString(response), SC_OK)));
  }

  private void mockEcsTlrSettings(TlrSettings tlrSettings, String requesterTenantId) {
    wireMockServer.stubFor(WireMock.get(urlMatching(TLR_SETTINGS_URL))
      .withHeader(HEADER_TENANT, equalTo(requesterTenantId))
      .willReturn(jsonResponse(asJsonString(tlrSettings), SC_OK)));
  }
}
