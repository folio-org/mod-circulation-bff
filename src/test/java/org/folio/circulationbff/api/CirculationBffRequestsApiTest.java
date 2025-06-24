package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.apache.http.HttpStatus.SC_OK;
import static org.folio.circulationbff.util.MockHelper.TLR_ALLOWED_SERVICE_POINT_URL;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.Request;
import org.folio.circulationbff.domain.dto.User;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.github.tomakehurst.wiremock.client.WireMock;

import lombok.SneakyThrows;

class CirculationBffRequestsApiTest extends BaseIT {
  private static final String ALLOWED_SERVICE_POINT_PATH = "/circulation-bff/requests/allowed" +
    "-service-points";
  private static final String CIRCULATION_ALLOWED_SERVICE_POINT_URL = "/circulation/requests" +
    "/allowed-service-points";
  private static final String USERS_URL = "/users";
  private static final String CIRCULATION_REQUESTS_URL = "/circulation/requests";

  @Test
  @SneakyThrows
  void callsCirculationWhenEcsTlrDisabled() {
    var userTenant = new UserTenant(UUID.randomUUID().toString(), TENANT_ID_COLLEGE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockHelper.mockUserTenants(userTenant, TENANT_ID_COLLEGE);
    mockHelper.mockEcsTlrCirculationSettings(false, TENANT_ID_COLLEGE);

    var allowedSpResponseConsortium = new AllowedServicePoints();
    allowedSpResponseConsortium.setHold(Set.of(
      mockHelper.buildAllowedServicePoint("SP_consortium_1"),
      mockHelper.buildAllowedServicePoint("SP_consortium_2")));
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
      .andExpect(jsonPath("$.Hold[*].name",
        containsInAnyOrder("SP_consortium_1", "SP_consortium_2")));

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
    mockHelper.mockUserTenants(userTenant, TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);
    mockHelper.mockAllowedServicePoints(TENANT_ID_CONSORTIUM);

    var operation = "create";
    var instanceId = UUID.randomUUID();
    var requestId = UUID.randomUUID();
    var patronGroupId = UUID.randomUUID();

    var allowedSpResponseConsortium = new AllowedServicePoints();
    allowedSpResponseConsortium.setHold(Set.of(
      mockHelper.buildAllowedServicePoint("SP_consortium_1"),
      mockHelper.buildAllowedServicePoint("SP_consortium_2")));
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
      .andExpect(jsonPath("$.Hold[*].name",
        containsInAnyOrder("SP_consortium_1", "SP_consortium_2")));

    wireMockServer.verify(getRequestedFor(urlPathEqualTo(TLR_ALLOWED_SERVICE_POINT_URL))
      .withQueryParam("requestId", equalTo(requestId.toString()))
      .withQueryParam("instanceId", equalTo(instanceId.toString()))
      .withQueryParam("operation", equalTo(operation))
    );
  }

  @Test
  @SneakyThrows
  void allowedSpCallsDataTenantCirculationWhenEcsTlrEnabledWithoutRequestId() {
    var userTenant = new UserTenant(UUID.randomUUID().toString(), TENANT_ID_COLLEGE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockHelper.mockUserTenants(userTenant, TENANT_ID_COLLEGE);
    mockHelper.mockEcsTlrCirculationSettings(true, TENANT_ID_COLLEGE);

    User user = new User().patronGroup(UUID.randomUUID().toString());
    wireMockServer.stubFor(WireMock.get(urlMatching(USERS_URL + ".*"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(asJsonString(user), SC_OK)));

    Request request = new Request().ecsRequestPhase(null);
    wireMockServer.stubFor(WireMock.get(urlMatching(CIRCULATION_REQUESTS_URL + ".*"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(asJsonString(request), SC_OK)));

    var allowedSpResponseConsortium = new AllowedServicePoints();
    allowedSpResponseConsortium.setHold(Set.of(
      mockHelper.buildAllowedServicePoint("SP_consortium_1"),
      mockHelper.buildAllowedServicePoint("SP_consortium_2")));

    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(TLR_ALLOWED_SERVICE_POINT_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(asJsonString(allowedSpResponseConsortium), SC_OK)));

    var operation = "create";
    var instanceId = UUID.randomUUID();
    var requesterId = UUID.randomUUID();

    mockMvc.perform(get(ALLOWED_SERVICE_POINT_PATH)
        .queryParam("operation", "create")
        .queryParam("instanceId", instanceId.toString())
        .queryParam("requesterId", requesterId.toString())
        .headers(buildHeaders(TENANT_ID_COLLEGE))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.Page").doesNotExist())
      .andExpect(jsonPath("$.Hold").exists())
      .andExpect(jsonPath("$.Recall").doesNotExist())
      .andExpect(jsonPath("$.Hold[*].name",
        containsInAnyOrder("SP_consortium_1", "SP_consortium_2")));

    wireMockServer.verify(getRequestedFor(urlPathEqualTo(
      TLR_ALLOWED_SERVICE_POINT_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .withQueryParam("instanceId", equalTo(instanceId.toString()))
      .withQueryParam("operation", equalTo(operation))
    );
  }

  @Test
  @SneakyThrows
  void allowedSpCallsDataTenantCirculationWhenEcsTlrEnabledWithRequestIdWithoutECSPhase() {
    var userTenant = new UserTenant(UUID.randomUUID().toString(), TENANT_ID_COLLEGE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockHelper.mockUserTenants(userTenant, TENANT_ID_COLLEGE);
    mockHelper.mockEcsTlrCirculationSettings(true, TENANT_ID_COLLEGE);

    User user = new User().patronGroup(UUID.randomUUID().toString());
    wireMockServer.stubFor(WireMock.get(urlMatching(USERS_URL + ".*"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(asJsonString(user), SC_OK)));

    Request request = new Request().ecsRequestPhase(null);
    wireMockServer.stubFor(WireMock.get(urlMatching(CIRCULATION_REQUESTS_URL + ".*"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(asJsonString(request), SC_OK)));

    var allowedSpResponseConsortium = new AllowedServicePoints();
    allowedSpResponseConsortium.setHold(Set.of(
      mockHelper.buildAllowedServicePoint("SP_consortium_1"),
      mockHelper.buildAllowedServicePoint("SP_consortium_2")));
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(CIRCULATION_ALLOWED_SERVICE_POINT_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(asJsonString(allowedSpResponseConsortium), SC_OK)));

    var operation = "create";
    var instanceId = UUID.randomUUID();
    var requestId = UUID.randomUUID();
    var requesterId = UUID.randomUUID();

    mockMvc.perform(get(ALLOWED_SERVICE_POINT_PATH)
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
      .andExpect(jsonPath("$.Hold[*].name",
        containsInAnyOrder("SP_consortium_1", "SP_consortium_2")));

    wireMockServer.verify(getRequestedFor(urlPathEqualTo(
      CIRCULATION_ALLOWED_SERVICE_POINT_URL))
      .withQueryParam("requestId", equalTo(requestId.toString()))
      .withQueryParam("instanceId", equalTo(instanceId.toString()))
      .withQueryParam("operation", equalTo(operation))
    );
  }

  @Test
  @SneakyThrows
  void allowedSpCallsDataTenantCirculationWhenEcsTlrEnabledWithRequestIdWithECSPhase() {
    var userTenant = new UserTenant(UUID.randomUUID().toString(), TENANT_ID_COLLEGE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockHelper.mockUserTenants(userTenant, TENANT_ID_COLLEGE);
    mockHelper.mockEcsTlrCirculationSettings(true, TENANT_ID_COLLEGE);

    User user = new User().patronGroup(UUID.randomUUID().toString());
    wireMockServer.stubFor(WireMock.get(urlMatching(USERS_URL + ".*"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(asJsonString(user), SC_OK)));

    Request request = new Request().ecsRequestPhase(Request.EcsRequestPhaseEnum.PRIMARY);
    wireMockServer.stubFor(WireMock.get(urlMatching(CIRCULATION_REQUESTS_URL + ".*"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(asJsonString(request), SC_OK)));

    mockHelper.mockAllowedServicePoints(TENANT_ID_CONSORTIUM);

    var operation = "create";
    var instanceId = UUID.randomUUID();
    var requestId = UUID.randomUUID();
    var requesterId = UUID.randomUUID();

    mockMvc.perform(get(ALLOWED_SERVICE_POINT_PATH)
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
      .andExpect(jsonPath("$.Hold[*].name",
        containsInAnyOrder("SP_consortium_1", "SP_consortium_2")));

    wireMockServer.verify(getRequestedFor(urlPathEqualTo(
      TLR_ALLOWED_SERVICE_POINT_URL))
      .withQueryParam("requestId", equalTo(requestId.toString()))
      .withQueryParam("instanceId", equalTo(instanceId.toString()))
      .withQueryParam("operation", equalTo(operation))
    );
  }

}
