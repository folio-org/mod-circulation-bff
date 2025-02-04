package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.folio.circulationbff.domain.dto.EcsRequestExternal.FulfillmentPreferenceEnum.HOLD_SHELF;
import static org.folio.circulationbff.domain.dto.EcsRequestExternal.RequestLevelEnum.ITEM;
import static org.folio.circulationbff.domain.dto.EcsRequestExternal.RequestLevelEnum.TITLE;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.List;

import org.folio.circulationbff.domain.dto.CirculationSettings;
import org.folio.circulationbff.domain.dto.CirculationSettingsResponse;
import org.folio.circulationbff.domain.dto.CirculationSettingsValue;
import org.folio.circulationbff.domain.dto.ConsortiumItem;
import org.folio.circulationbff.domain.dto.EcsRequestExternal;
import org.folio.circulationbff.domain.dto.EcsRequestExternal.RequestLevelEnum;
import org.folio.circulationbff.domain.dto.EcsTlr;
import org.folio.circulationbff.domain.dto.Request;
import org.folio.circulationbff.domain.dto.TlrSettings;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.folio.circulationbff.domain.dto.UserTenantCollection;
import org.folio.circulationbff.service.impl.TenantServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.github.tomakehurst.wiremock.client.WireMock;

import lombok.SneakyThrows;

class EcsExternalRequestApiTest extends BaseIT {
  private static final String TLR_CREATE_ECS_EXTERNAL_REQUEST_URL =
    "/tlr/create-ecs-request-external";
  private static final String CIRCULATION_BFF_CREATE_ECS_EXTERNAL_REQUEST_URL =
    "/circulation-bff/create-ecs-request-external";
  private static final String USER_TENANTS_URL = "/user-tenants";
  private static final String CIRCULATION_REQUEST_URL_TEMPLATE = "/circulation/requests/%s";
  private static final String TLR_SETTINGS_URL = "/tlr/settings";
  private static final String CIRCULATION_SETTINGS_URL = "/circulation/settings";
  private static final String SEARCH_ITEM_URL_TEMPLATE = "/search/consortium/item/%s";

  private static final String REQUESTER_ID = randomId();
  private static final String ITEM_ID = randomId();
  private static final String INSTANCE_ID = randomId();
  private static final String HOLDING_ID = randomId();
  private static final String PICKUP_SERVICE_POINT_ID = randomId();
  private static final String PRIMARY_REQUEST_ID = randomId();
  private static final Date REQUEST_DATE = new Date();

  @BeforeEach
  void beforeEach() {
    TenantServiceImpl.clearCentralTenantIdCache();
  }

  @Test
  @SneakyThrows
  void createExternalItemLevelEcsTlr() {
    EcsRequestExternal initialRequest =  buildEcsRequestExternal(ITEM);
    EcsRequestExternal expectedRequestBody = buildEcsRequestExternal(ITEM)
      .holdingsRecordId(HOLDING_ID)
      .instanceId(INSTANCE_ID);

    Request mockPrimaryRequest = new Request().id(PRIMARY_REQUEST_ID);

    mockItemSearch(ITEM_ID);
    mockUserTenants();
    mockEcsTlrFeatureSettings(true);
    mockEcsTlrExternalRequestCreating(expectedRequestBody);
    mockPrimaryRequest(mockPrimaryRequest);

    createExternalRequest(initialRequest)
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(mockPrimaryRequest)));

    wireMockServer.verify(1, getRequestedFor(urlPathMatching(String.format(SEARCH_ITEM_URL_TEMPLATE, ITEM_ID)))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(1, getRequestedFor(urlPathMatching(USER_TENANTS_URL))
      .withQueryParam("limit", equalTo("1")));
    wireMockServer.verify(1, getRequestedFor(urlPathMatching(TLR_SETTINGS_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathMatching(CIRCULATION_SETTINGS_URL)));
    wireMockServer.verify(1, postRequestedFor(urlPathMatching(TLR_CREATE_ECS_EXTERNAL_REQUEST_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .withRequestBody(equalToJson(asJsonString(expectedRequestBody))));
    wireMockServer.verify(1, getRequestedFor(urlPathMatching(String.format(
      CIRCULATION_REQUEST_URL_TEMPLATE, PRIMARY_REQUEST_ID))));
  }

  @Test
  @SneakyThrows
  void createExternalTitleLevelEcsTlr() {
    EcsRequestExternal initialRequest =  buildEcsRequestExternal(TITLE);
    Request mockPrimaryRequest = new Request().id(PRIMARY_REQUEST_ID);

    mockUserTenants();
    mockEcsTlrFeatureSettings(true);
    mockEcsTlrExternalRequestCreating(initialRequest);
    mockPrimaryRequest(mockPrimaryRequest);

    createExternalRequest(initialRequest)
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(mockPrimaryRequest)));

    wireMockServer.verify(0, getRequestedFor(urlPathMatching(String.format(SEARCH_ITEM_URL_TEMPLATE, ITEM_ID))));
    wireMockServer.verify(1, getRequestedFor(urlPathMatching(USER_TENANTS_URL))
      .withQueryParam("limit", equalTo("1")));
    wireMockServer.verify(1, getRequestedFor(urlPathMatching(TLR_SETTINGS_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathMatching(CIRCULATION_SETTINGS_URL)));
    wireMockServer.verify(1, postRequestedFor(urlPathMatching(TLR_CREATE_ECS_EXTERNAL_REQUEST_URL))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .withRequestBody(equalToJson(asJsonString(initialRequest))));
    wireMockServer.verify(1, getRequestedFor(urlPathMatching(String.format(
      CIRCULATION_REQUEST_URL_TEMPLATE, PRIMARY_REQUEST_ID))));
  }

  @Test
  @SneakyThrows
  void createExternalMediatedRequest() {
    EcsRequestExternal initialRequest =  buildEcsRequestExternal(TITLE);

    mockUserTenants();
    mockCirculationSettings(true);

    createExternalRequest(initialRequest, TENANT_ID_SECURE)
      .andExpect(status().isOk());

    wireMockServer.verify(0, getRequestedFor(urlPathMatching(TLR_SETTINGS_URL)));
    wireMockServer.verify(1, getRequestedFor(urlPathMatching(CIRCULATION_SETTINGS_URL)));
    wireMockServer.verify(1, getRequestedFor(urlPathMatching(USER_TENANTS_URL))
      .withQueryParam("limit", equalTo("1")));
  }

  @Test
  @SneakyThrows
  void createExternalTitleLevelCirculationRequest() {
    EcsRequestExternal initialRequest =  buildEcsRequestExternal(TITLE);

    mockUserTenants();
    mockEcsTlrFeatureSettings(false);

    createExternalRequest(initialRequest)
      .andExpect(status().isOk());

    wireMockServer.verify(1, getRequestedFor(urlPathMatching(TLR_SETTINGS_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathMatching(CIRCULATION_SETTINGS_URL)));
    wireMockServer.verify(1, getRequestedFor(urlPathMatching(USER_TENANTS_URL))
      .withQueryParam("limit", equalTo("1")));
  }

  @Test
  @SneakyThrows
  void createExternalItemLevelCirculationRequest() {
    EcsRequestExternal initialRequest =  buildEcsRequestExternal(ITEM);

    mockItemSearch(ITEM_ID);
    mockUserTenants();
    mockEcsTlrFeatureSettings(false);

    createExternalRequest(initialRequest)
      .andExpect(status().isOk());

    wireMockServer.verify(1, getRequestedFor(urlPathMatching(TLR_SETTINGS_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathMatching(CIRCULATION_SETTINGS_URL)));
    wireMockServer.verify(1, getRequestedFor(urlPathMatching(USER_TENANTS_URL))
      .withQueryParam("limit", equalTo("1")));
  }

  private static void mockUserTenants() {
    UserTenant userTenant = new UserTenant()
      .centralTenantId(TENANT_ID_CONSORTIUM)
      .tenantId("random_tenant");
    UserTenantCollection userTenants = new UserTenantCollection(List.of(userTenant), 1);

    wireMockServer.stubFor(get(urlPathEqualTo(USER_TENANTS_URL))
      .withQueryParam("limit", equalTo("1"))
      .willReturn(jsonResponse(asJsonString(userTenants), SC_OK)));
  }

  private static EcsRequestExternal buildEcsRequestExternal(RequestLevelEnum requestLevel) {
    EcsRequestExternal request = new EcsRequestExternal()
      .requestLevel(requestLevel)
      .requesterId(REQUESTER_ID)
      .requestDate(REQUEST_DATE)
      .fulfillmentPreference(HOLD_SHELF)
      .pickupServicePointId(PICKUP_SERVICE_POINT_ID);

    if (requestLevel == ITEM) {
      request.setItemId(ITEM_ID);
    } else if (requestLevel == TITLE) {
      request.setInstanceId(INSTANCE_ID);
    }

    return request;
  }

  @SneakyThrows
  private ResultActions createExternalRequest(EcsRequestExternal requestExternal) {
    return createExternalRequest(requestExternal, TENANT_ID_CONSORTIUM);
  }

  @SneakyThrows
  private ResultActions createExternalRequest(EcsRequestExternal requestExternal, String tenantId) {
    return mockMvc.perform(post(CIRCULATION_BFF_CREATE_ECS_EXTERNAL_REQUEST_URL)
      .headers(buildHeaders(tenantId))
      .content(asJsonString(requestExternal)));
  }

  private static void mockEcsTlrExternalRequestCreating(EcsRequestExternal requestBody) {
    EcsTlr ecsTlr = new EcsTlr()
      .id(randomId())
      .primaryRequestId(PRIMARY_REQUEST_ID);

    wireMockServer.stubFor(WireMock.post(urlMatching(TLR_CREATE_ECS_EXTERNAL_REQUEST_URL))
      .withRequestBody(equalToJson(asJsonString(requestBody)))
      .willReturn(jsonResponse(asJsonString(ecsTlr), SC_CREATED)));
  }

  private static void mockPrimaryRequest(Request mockPrimaryRequest) {
    wireMockServer.stubFor(get(urlMatching(String.format(
      CIRCULATION_REQUEST_URL_TEMPLATE, PRIMARY_REQUEST_ID)))
      .willReturn(jsonResponse(asJsonString(mockPrimaryRequest), SC_OK)));
  }

  private static void mockEcsTlrFeatureSettings(boolean isEcsTlrEnabled) {
    wireMockServer.stubFor(get(urlPathMatching(TLR_SETTINGS_URL))
      .willReturn(jsonResponse(asJsonString(new TlrSettings(isEcsTlrEnabled)), SC_OK)));
  }

  private static void mockCirculationSettings(boolean isEcsTlrEnabled) {
    CirculationSettingsResponse mockResponse = new CirculationSettingsResponse()
      .totalRecords(1)
      .circulationSettings(List.of(new CirculationSettings()
        .id(randomId())
        .name("ecsTlrFeature")
        .value(new CirculationSettingsValue().enabled(isEcsTlrEnabled))));

    wireMockServer.stubFor(get(urlPathMatching(CIRCULATION_SETTINGS_URL))
        .withQueryParam("query", equalTo("name=ecsTlrFeature"))
      .willReturn(jsonResponse(asJsonString(mockResponse), SC_OK)));
  }

  private static void mockItemSearch(String itemId) {
    ConsortiumItem consortiumItem = new ConsortiumItem()
      .id(itemId)
      .holdingsRecordId(HOLDING_ID)
      .instanceId(INSTANCE_ID);

    wireMockServer.stubFor(get(urlPathMatching(String.format(SEARCH_ITEM_URL_TEMPLATE, itemId)))
      .willReturn(jsonResponse(asJsonString(consortiumItem), SC_OK)));
  }
}
