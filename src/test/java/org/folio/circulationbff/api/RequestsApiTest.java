package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.folio.circulationbff.util.TestUtils.mockUserTenants;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.folio.circulationbff.domain.dto.BffRequest;
import org.folio.circulationbff.domain.dto.CirculationSettings;
import org.folio.circulationbff.domain.dto.CirculationSettingsResponse;
import org.folio.circulationbff.domain.dto.CirculationSettingsValue;
import org.folio.circulationbff.domain.dto.EcsTlr;
import org.folio.circulationbff.domain.dto.Request;
import org.folio.circulationbff.domain.dto.TlrSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import lombok.SneakyThrows;

class RequestsApiTest extends BaseIT {
  private static final String CIRCULATION_SETTINGS_URL = "/circulation/settings";
  private static final String CIRCULATION_REQUEST_URL = "/circulation/requests";
  private static final String ECS_TLR_REQUEST_URL = "/tlr/ecs-tlr";
  private static final String TLR_SETTINGS_URL = "/tlr/settings";
  private static final String REQUESTS_PATH = "/circulation-bff/requests";

  @BeforeEach
  public void beforeEach() {
    wireMockServer.resetAll();
  }

  @Test
  @SneakyThrows
  void createCirculationRequestInDataTenant() {
    mockUserTenants(wireMockServer, TENANT_ID_COLLEGE, UUID.randomUUID());
    mockEcsTlrCirculationSettings(true);
    mockEcsTlrSettings(false);

    for (StubMapping mapping : wireMockServer.getStubMappings()) {
      System.out.println(mapping);
    }

    var request = new BffRequest()
      .requesterId(UUID.randomUUID().toString())
      .requestType(BffRequest.RequestTypeEnum.PAGE)
      .requestDate(new Date())
      .requestLevel(BffRequest.RequestLevelEnum.ITEM)
      .instanceId(UUID.randomUUID().toString())
      .fulfillmentPreference(BffRequest.FulfillmentPreferenceEnum.HOLD_SHELF)
      .pickupServicePointId(UUID.randomUUID().toString());

    wireMockServer.stubFor(WireMock.post(urlMatching(CIRCULATION_REQUEST_URL))
        .withRequestBody(equalToJson(asJsonString(request)))
      .willReturn(jsonResponse(asJsonString(new Request().id(UUID.randomUUID().toString())), SC_CREATED)));

    doPostWithTenant(REQUESTS_PATH, request, TENANT_ID_COLLEGE)
      .andExpect(MockMvcResultMatchers.status().isCreated())
      .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists());

    wireMockServer.verify(postRequestedFor(urlMatching(CIRCULATION_REQUEST_URL)));
  }

  @Test
  @SneakyThrows
  void createEcsTlrRequestInCentralTenant() {
    mockUserTenants(wireMockServer, TENANT_ID_CONSORTIUM, UUID.randomUUID());
    mockEcsTlrCirculationSettings(true);
    mockEcsTlrSettings(true);

    var request = new BffRequest()
      .requesterId(UUID.randomUUID().toString())
      .requestType(BffRequest.RequestTypeEnum.PAGE)
      .requestDate(new Date())
      .requestLevel(BffRequest.RequestLevelEnum.TITLE)
      .instanceId(UUID.randomUUID().toString())
      .fulfillmentPreference(BffRequest.FulfillmentPreferenceEnum.HOLD_SHELF)
      .pickupServicePointId(UUID.randomUUID().toString());

    var primaryRequestId = UUID.randomUUID().toString();
    wireMockServer.stubFor(WireMock.post(urlMatching(ECS_TLR_REQUEST_URL))
      .willReturn(jsonResponse(asJsonString(
        new EcsTlr().id(UUID.randomUUID().toString())
          .primaryRequestId(primaryRequestId)), SC_CREATED)));

    wireMockServer.stubFor(WireMock.get(urlMatching(String.format("%s/%s",
        CIRCULATION_REQUEST_URL, primaryRequestId)))
      .willReturn(jsonResponse(asJsonString(
        new Request().id(UUID.randomUUID().toString())), SC_OK)));

    doPostWithTenant(REQUESTS_PATH, request, TENANT_ID_CONSORTIUM)
      .andExpect(MockMvcResultMatchers.status().isCreated())
      .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists());

    wireMockServer.verify(postRequestedFor(urlMatching(ECS_TLR_REQUEST_URL)));
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
}
