package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.AllowedServicePoints1Inner;
import org.folio.circulationbff.domain.dto.CirculationSettings;
import org.folio.circulationbff.domain.dto.CirculationSettingsResponse;
import org.folio.circulationbff.domain.dto.CirculationSettingsValue;
import org.folio.circulationbff.domain.dto.Instance;
import org.folio.circulationbff.domain.dto.InstanceSearchResult;
import org.folio.circulationbff.domain.dto.TlrSettings;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.folio.circulationbff.domain.dto.UserTenantCollection;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.github.tomakehurst.wiremock.client.WireMock;

import lombok.SneakyThrows;

class CirculationBffRequestsApiTest extends BaseIT {
  private static final String SEARCH_INSTANCES_URL_PATH =
    "/circulation-bff/requests/search-instances";
  private static final String SEARCH_INSTANCES_MOD_SEARCH_URL_PATH = "/search/instances";
  private static final String ALLOWED_SERVICE_POINT_PATH = "/circulation-bff/requests/allowed" +
    "-service-points";
  private static final String CIRCULATION_SETTINGS_URL = "/circulation/settings";
  private static final String TLR_SETTINGS_URL = "/tlr/settings";
  private static final String USER_TENANTS_URL = "/user-tenants";
  private static final String TLR_ALLOWED_SERVICE_POINT_URL = "/tlr/allowed-service-points";
  private static final String CIRCULATION_ALLOWED_SERVICE_POINT_URL = "/circulation/requests" +
    "/allowed-service-points";

  @Test
  @SneakyThrows
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
  void returnValidationErrorWhenClientThrowsUnprocessableEntity() {
    mockEcsTlrCirculationSettings(false);

    var userTenant = new UserTenant(UUID.randomUUID().toString(), TENANT_ID_COLLEGE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockUserTenants(userTenant, TENANT_ID_COLLEGE);
    mockEcsTlrCirculationSettings(true);
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(CIRCULATION_ALLOWED_SERVICE_POINT_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse("{}", HttpStatus.SC_UNPROCESSABLE_ENTITY)));

    mockMvc.perform(
        get(ALLOWED_SERVICE_POINT_PATH)
          .queryParam("operation", "create")
          .headers(buildHeaders(TENANT_ID_COLLEGE))
          .contentType(MediaType.APPLICATION_JSON))
      .andExpectAll(status().is4xxClientError(),
        jsonPath("$.errors[0].code", is("VALIDATION_ERROR")));;
  }

  @Test
  @SneakyThrows
  void searchInstancesReturnsOkStatus() {
    String instanceId = randomId();
    InstanceSearchResult mockSearchResponse = new InstanceSearchResult()
      .addInstancesItem(new Instance().id(instanceId))
      .totalRecords(1);

    wireMockServer.stubFor(WireMock.get(urlPathMatching(SEARCH_INSTANCES_MOD_SEARCH_URL_PATH))
      .withQueryParam("query", equalTo("id==" + instanceId))
      .withQueryParam("expandAll", equalTo("true"))
      .willReturn(jsonResponse(mockSearchResponse, HttpStatus.SC_OK)));

    mockMvc.perform(
      get(SEARCH_INSTANCES_URL_PATH)
        .queryParam("query", "id==" + instanceId)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("instances[0].id", is(instanceId)))
      .andExpect(jsonPath("totalRecords", is(1)));
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
