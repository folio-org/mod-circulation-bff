package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.AllowedServicePoints1Inner;
import org.folio.circulationbff.domain.dto.CirculationSettings;
import org.folio.circulationbff.domain.dto.CirculationSettingsResponse;
import org.folio.circulationbff.domain.dto.CirculationSettingsValue;
import org.folio.circulationbff.domain.dto.TlrSettings;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.folio.circulationbff.domain.dto.UserTenantCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.github.tomakehurst.wiremock.client.WireMock;

class CirculationBffRequestsApiTest extends BaseIT {
  private static final String SEARCH_INSTANCES_URL_PATH =
    "/circulation-bff/requests/search-instances";

  private static final String ALLOWED_SERVICE_POINT_PATH = "/circulation-bff/requests/allowed" +
    "-service-points";
  private static final String SEARCH_INSTANCES_QUERY_PARAM_TMP = "query=%s";
  private static final String URL_TMP = "%s?%s";

  private static final String CIRCULATION_SETTINGS_URL = "/circulation/settings";
  private static final String TLR_SETTINGS_URL = "/tlr/settings";
  private static final String USER_TENANTS_URL = "/user-tenants";
  private static final String TLR_ALLOWED_SERVICE_POINT_URL = "/tlr/allowed-service-points";
  private static final String CIRCULATION_ALLOWED_SERVICE_POINT_URL = "/circulation/requests" +
    "/allowed-service-points";

  @BeforeEach
  public void beforeEach() {
    wireMockServer.resetAll();
  }

  @Test
  void callsModTlrWhenEcsTlrEnabledInCentralTenant() {

    var userTenant = new UserTenant(UUID.randomUUID().toString(), TENANT_ID_CONSORTIUM);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockUserTenants(userTenant, TENANT_ID_CONSORTIUM);
    mockEcsTlrSettings(true);
    mockAllowedServicePoints(TENANT_ID_CONSORTIUM);

    var operation = "create";
    var instanceId = UUID.randomUUID();
    var requestId = UUID.randomUUID();
    var patronGroupId = UUID.randomUUID();

    var allowedSpResponseConsortium = new AllowedServicePoints();
    allowedSpResponseConsortium.setHold(Set.of(
      buildAllowedServicePoint("SP_consortium_1"),
      buildAllowedServicePoint("SP_consortium_2")));
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(TLR_ALLOWED_SERVICE_POINT_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(asJsonString(allowedSpResponseConsortium), SC_OK)));

    doGet(
      ALLOWED_SERVICE_POINT_PATH + format("?operation=create&requestId=%s&instanceId=%s&patronGroupId=%s",
        requestId, instanceId, patronGroupId), TENANT_ID_CONSORTIUM)
      .expectStatus().isEqualTo(200)
      .expectBody().json("{}");

    wireMockServer.verify(getRequestedFor(urlPathEqualTo(
      TLR_ALLOWED_SERVICE_POINT_URL))
      .withQueryParam("requestId", equalTo(requestId.toString()))
      .withQueryParam("instanceId", equalTo(instanceId.toString()))
      .withQueryParam("operation", equalTo(operation))
    );
  }

  @Test
  void callsCirculationWhenEcsTlrDisabledOnDataTenant() {
    var userTenant = new UserTenant(UUID.randomUUID().toString(), TENANT_ID_COLLEGE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockUserTenants(userTenant, TENANT_ID_COLLEGE);
    mockEcsTlrCirculationSettings(true);
    mockAllowedServicePoints(TENANT_ID_COLLEGE);

    var operation = "create";
    var instanceId = UUID.randomUUID();
    var requestId = UUID.randomUUID();
    var patronGroupId = UUID.randomUUID();

    doGet(
      ALLOWED_SERVICE_POINT_PATH + format("?operation=create&requestId=%s&instanceId=%s&patronGroupId=%s",
        requestId, instanceId, patronGroupId), TENANT_ID_COLLEGE)
      .expectStatus().isEqualTo(200)
      .expectBody().json("{}");

    wireMockServer.verify(getRequestedFor(urlPathEqualTo(
      CIRCULATION_ALLOWED_SERVICE_POINT_URL))
      .withQueryParam("requestId", equalTo(requestId.toString()))
      .withQueryParam("instanceId", equalTo(instanceId.toString()))
      .withQueryParam("operation", equalTo(operation))
    );
  }

  @Test
  void searchInstancesReturnsOkStatus() throws Exception {
    mockMvc.perform(
        get(buildUrl(SEARCH_INSTANCES_URL_PATH, SEARCH_INSTANCES_QUERY_PARAM_TMP,
          format("id==%s", randomId())))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
  }

  private String buildUrl(String path, String parametersTemplate, String... parameters) {
    return format(URL_TMP, path, format(parametersTemplate, (Object[]) parameters));
  }

  private AllowedServicePoints1Inner buildAllowedServicePoint(String name) {
    return new AllowedServicePoints1Inner()
      .id(randomId())
      .name(name);
  }

  private void mockUserTenants(UserTenant userTenant, String requestTenant) {
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(USER_TENANTS_URL))
      .withQueryParam("limit", matching("\\d*"))
      .withHeader(HEADER_TENANT, equalTo(requestTenant))
      .willReturn(jsonResponse(asJsonString(new UserTenantCollection().addUserTenantsItem(userTenant)),
        SC_OK)));
  }

  private void mockEcsTlrCirculationSettings(boolean enabled) {
    var circulationSettingsResponse = new CirculationSettingsResponse();
    circulationSettingsResponse.setTotalRecords(1);
    circulationSettingsResponse.setCirculationSettings(List.of(
      new CirculationSettings()
        .name("ecsTlrFeature")
        .value(new CirculationSettingsValue().enabled(enabled))
    ));
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(CIRCULATION_SETTINGS_URL))
      .withQueryParam("query", equalTo("name=ecsTlrFeature"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(asJsonString(circulationSettingsResponse),
        SC_OK)));
  }

  private void mockEcsTlrSettings(boolean enabled) {
    TlrSettings tlrSettings = new TlrSettings();
    tlrSettings.setEcsTlrFeatureEnabled(enabled);
    wireMockServer.stubFor(WireMock.get(urlMatching(TLR_SETTINGS_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(asJsonString(tlrSettings), SC_OK)));
  }

  private void mockAllowedServicePoints(String requestTenant) {
    var allowedSpResponseConsortium = new AllowedServicePoints();
    allowedSpResponseConsortium.setHold(Set.of(
      buildAllowedServicePoint("SP_consortium_1"),
      buildAllowedServicePoint("SP_consortium_2")));
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(CIRCULATION_ALLOWED_SERVICE_POINT_URL))
      .withHeader(HEADER_TENANT, equalTo(requestTenant))
      .willReturn(jsonResponse(asJsonString(allowedSpResponseConsortium), SC_OK)));
  }
}
