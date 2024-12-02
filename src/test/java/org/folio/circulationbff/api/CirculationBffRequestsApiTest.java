package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.folio.circulationbff.domain.dto.User;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.folio.circulationbff.domain.dto.UserTenantCollection;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.github.tomakehurst.wiremock.client.WireMock;

import lombok.SneakyThrows;

class CirculationBffRequestsApiTest extends BaseIT {
  private static final String ALLOWED_SERVICE_POINT_PATH = "/circulation-bff/requests/allowed" +
    "-service-points";
  private static final String TLR_ALLOWED_SERVICE_POINT_URL = "/tlr/allowed-service-points";
  private static final String CIRCULATION_ALLOWED_SERVICE_POINT_URL = "/circulation/requests" +
    "/allowed-service-points";
  private static final String USERS_URL = "/users";

  @Test
  @SneakyThrows
  void callsCirculationWhenEcsTlrDisabled() {
    var userTenant = new UserTenant(UUID.randomUUID().toString(), TENANT_ID_COLLEGE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockUserTenants(userTenant, TENANT_ID_COLLEGE);
    mockEcsTlrCirculationSettings(false);

    var allowedSpResponseConsortium = new AllowedServicePoints();
    allowedSpResponseConsortium.setHold(Set.of(
      buildAllowedServicePoint("SP_consortium_1"),
      buildAllowedServicePoint("SP_consortium_2")));
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(CIRCULATION_ALLOWED_SERVICE_POINT_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(asJsonString(allowedSpResponseConsortium), SC_OK)));

    var operation = "create";
    var instanceId = UUID.randomUUID();
    var requestId = UUID.randomUUID();
    var patronGroupId = UUID.randomUUID();

    mockMvc.perform(
      get(ALLOWED_SERVICE_POINT_PATH)
        .queryParam("operation", "create")
        .queryParam("requestId", requestId.toString())
        .queryParam("instanceId", instanceId.toString())
        .queryParam("patronGroupId", patronGroupId.toString())
        .headers(buildHeaders(TENANT_ID_COLLEGE))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.Page").doesNotExist())
      .andExpect(jsonPath("$.Hold").exists())
      .andExpect(jsonPath("$.Recall").doesNotExist())
      .andExpect(jsonPath("$.Hold[*].name", containsInAnyOrder("SP_consortium_1", "SP_consortium_2")));

    wireMockServer.verify(getRequestedFor(urlPathEqualTo(
      CIRCULATION_ALLOWED_SERVICE_POINT_URL))
      .withQueryParam("requestId", equalTo(requestId.toString()))
      .withQueryParam("instanceId", equalTo(instanceId.toString()))
      .withQueryParam("operation", equalTo(operation))
    );
  }

  @Test
  @SneakyThrows
  void callsTlrWhenEcsTlrEnabledInCentralTenant() {
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

    mockMvc.perform(
      get(ALLOWED_SERVICE_POINT_PATH)
        .queryParam("operation", "create")
        .queryParam("requestId", requestId.toString())
        .queryParam("instanceId", instanceId.toString())
        .queryParam("patronGroupId", patronGroupId.toString())
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.Page").doesNotExist())
      .andExpect(jsonPath("$.Hold").exists())
      .andExpect(jsonPath("$.Recall").doesNotExist())
      .andExpect(jsonPath("$.Hold[*].name", containsInAnyOrder("SP_consortium_1", "SP_consortium_2")));

    wireMockServer.verify(getRequestedFor(urlPathEqualTo(TLR_ALLOWED_SERVICE_POINT_URL))
      .withQueryParam("requestId", equalTo(requestId.toString()))
      .withQueryParam("instanceId", equalTo(instanceId.toString()))
      .withQueryParam("operation", equalTo(operation))
    );
  }

  @Test
  @SneakyThrows
  void callsCirculationWhenEcsTlrEnabledOnDataTenant() {
    var userTenant = new UserTenant(UUID.randomUUID().toString(), TENANT_ID_COLLEGE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockUserTenants(userTenant, TENANT_ID_COLLEGE);
    mockEcsTlrCirculationSettings(true);

    User user = new User().patronGroup(UUID.randomUUID().toString());
    wireMockServer.stubFor(WireMock.get(urlMatching(USERS_URL + ".*"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(asJsonString(user), SC_OK)));

    mockAllowedServicePoints(TENANT_ID_CONSORTIUM);

    var operation = "create";
    var instanceId = UUID.randomUUID();
    var requestId = UUID.randomUUID();
    var requesterId = UUID.randomUUID();

    mockMvc.perform(
      get(ALLOWED_SERVICE_POINT_PATH)
        .queryParam("operation", "create")
        .queryParam("requestId", requestId.toString())
        .queryParam("instanceId", instanceId.toString())
        .queryParam("requesterId", requesterId.toString())
        .headers(buildHeaders(TENANT_ID_COLLEGE))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.Page").doesNotExist())
      .andExpect(jsonPath("$.Hold").exists())
      .andExpect(jsonPath("$.Recall").doesNotExist())
      .andExpect(jsonPath("$.Hold[*].name", containsInAnyOrder("SP_consortium_1", "SP_consortium_2")));

    wireMockServer.verify(getRequestedFor(urlPathEqualTo(
      TLR_ALLOWED_SERVICE_POINT_URL))
      .withQueryParam("requestId", equalTo(requestId.toString()))
      .withQueryParam("instanceId", equalTo(instanceId.toString()))
      .withQueryParam("operation", equalTo(operation))
    );
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
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(TLR_ALLOWED_SERVICE_POINT_URL))
      .withHeader(HEADER_TENANT, equalTo(requestTenant))
      .willReturn(jsonResponse(asJsonString(allowedSpResponseConsortium), SC_OK)));
  }
}
