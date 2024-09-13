package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.lang.String.format;
import static org.apache.http.HttpStatus.SC_OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.AllowedServicePoints1Inner;
import org.folio.circulationbff.domain.dto.TlrSettings;
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

  private static final String TLR_SETTINGS_URL = "/tlr/settings";
  private static final String TLR_ALLOWED_SERVICE_POINT_URL = "/tlr/allowed-service-points";

  @BeforeEach
  public void beforeEach() {
    wireMockServer.resetAll();
  }

  @Test
  void allowedServicePointsReturnsOkStatus() {
    TlrSettings tlrSettings = new TlrSettings();
    tlrSettings.setEcsTlrFeatureEnabled(true);

    var allowedSpResponseConsortium = new AllowedServicePoints();
    allowedSpResponseConsortium.setHold(Set.of(
      buildAllowedServicePoint("SP_consortium_1"),
      buildAllowedServicePoint("SP_consortium_2")));

    wireMockServer.stubFor(WireMock.get(urlMatching(TLR_SETTINGS_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(asJsonString(tlrSettings), SC_OK)));


    var operation = "create";
    var instanceId = UUID.randomUUID();
    var requestId = UUID.randomUUID();
    var patronGroupId = UUID.randomUUID();

    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(TLR_ALLOWED_SERVICE_POINT_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(asJsonString(allowedSpResponseConsortium), SC_OK)));

    doGet(
      ALLOWED_SERVICE_POINT_PATH + format("?operation=create&requestId=%s&instanceId=%s&patronGroupId=%s",
        requestId, instanceId, patronGroupId))
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
}
