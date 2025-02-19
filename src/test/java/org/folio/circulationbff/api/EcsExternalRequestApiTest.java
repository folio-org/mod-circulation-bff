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

import org.folio.circulationbff.domain.dto.ConsortiumItem;
import org.folio.circulationbff.domain.dto.EcsRequestExternal;
import org.folio.circulationbff.domain.dto.EcsRequestExternal.RequestLevelEnum;
import org.folio.circulationbff.domain.dto.EcsTlr;
import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.folio.circulationbff.domain.dto.Request;
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
  private static final String SEARCH_ITEM_URL_TEMPLATE = "/search/consortium/item/%s";
  private static final String MEDIATED_REQUESTS_URL = "/requests-mediated/mediated-requests";

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
    mockEcsTlrExternalRequestCreating(expectedRequestBody);
    mockPrimaryRequest(mockPrimaryRequest);

    createExternalRequest(initialRequest)
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(mockPrimaryRequest)));

    wireMockServer.verify(1, getRequestedFor(urlPathMatching(String.format(SEARCH_ITEM_URL_TEMPLATE, ITEM_ID)))
      .withHeader(TENANT, equalTo(TENANT_ID_CONSORTIUM)));
    wireMockServer.verify(1, getRequestedFor(urlPathMatching(USER_TENANTS_URL))
      .withQueryParam("limit", equalTo("1")));
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
    mockEcsTlrExternalRequestCreating(initialRequest);
    mockPrimaryRequest(mockPrimaryRequest);

    createExternalRequest(initialRequest)
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(mockPrimaryRequest)));

    wireMockServer.verify(0, getRequestedFor(urlPathMatching(String.format(SEARCH_ITEM_URL_TEMPLATE, ITEM_ID))));
    wireMockServer.verify(1, getRequestedFor(urlPathMatching(USER_TENANTS_URL))
      .withQueryParam("limit", equalTo("1")));
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
//    mockMediatedRequest(buildMediatedRequest(initialRequest));
    mockMediatedRequest(buildMediatedRequest());

    createExternalRequest(initialRequest, TENANT_ID_SECURE)
      .andExpect(status().isOk());
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

  private static MediatedRequest buildMediatedRequest(EcsRequestExternal externalRequest) {
    return new MediatedRequest();
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

  private static void mockItemSearch(String itemId) {
    ConsortiumItem consortiumItem = new ConsortiumItem()
      .id(itemId)
      .holdingsRecordId(HOLDING_ID)
      .instanceId(INSTANCE_ID);

    wireMockServer.stubFor(get(urlPathMatching(String.format(SEARCH_ITEM_URL_TEMPLATE, itemId)))
      .willReturn(jsonResponse(asJsonString(consortiumItem), SC_OK)));
  }

  private static void mockMediatedRequest(MediatedRequest mediatedRequest) {
    wireMockServer.stubFor(WireMock.post(urlPathMatching(MEDIATED_REQUESTS_URL))
      .willReturn(jsonResponse(asJsonString(mediatedRequest), SC_OK)));
  }

  @SneakyThrows
  private static MediatedRequest buildMediatedRequest() {
    return OBJECT_MAPPER.readValue("""
      {
        "id": "00eabb9c-d850-4d79-9488-64c438827eb2",
        "requestLevel": "Item",
        "requestType": "Page",
        "requestDate": "2025-02-05T16:45:09.816+00:00",
        "requesterId": "06d45eab-46f5-4fdb-8cc3-cdec4461dcd1",
        "requester": {
          "firstName": "Secure",
          "lastName": "Requester ",
          "barcode": "S",
          "patronGroupId": "3684a786-6671-4268-8ed0-9db82ebca60b",
          "patronGroup": {
            "id": "3684a786-6671-4268-8ed0-9db82ebca60b",
            "group": "staff",
            "desc": "Staff Member"
          }
        },
        "instanceId": "2c9b1261-f57f-4dd8-a20e-f08f1cd80c73",
        "instance": {
          "title": "MODREQMED-76",
          "identifiers": [],
          "contributorNames": [],
          "publication": [],
          "editions": [],
          "hrid": "in00000000027"
        },
        "holdingsRecordId": "d4a396ff-7a2e-4f89-9ce3-08cbbc02268b",
        "itemId": "a05f8be0-0ff1-4423-b0a4-267e5414a032",
        "item": {
          "barcode": "MODREQMED-76-2",
          "location": {
            "name": "College",
            "libraryName": "College",
            "code": "1"
          },
          "status": "In transit",
          "callNumberComponents": {}
        },
        "mediatedWorkflow": "Private request",
        "mediatedRequestStatus": "New",
        "mediatedRequestStep": "Awaiting confirmation",
        "status": "Open - Not yet filled",
        "fulfillmentPreference": "Hold Shelf",
        "pickupServicePointId": "3a40852d-49fd-4df2-a1f9-6e2641a6e91f",
        "pickupServicePoint": {
          "name": "Circ Desk 1",
          "code": "cd1",
          "discoveryDisplayName": "Circulation Desk -- Hallway",
          "pickupLocation": true
        },
        "confirmedRequestId": "1807d75c-1f04-49f1-8e0b-6c4221f8ae04",
        "metadata": {
          "createdDate": "2025-02-05T16:45:10.158+00:00",
          "createdByUserId": "3ad2a24b-d349-4f8d-b38e-1ed8628da348",
          "updatedDate": "2025-02-05T16:45:10.158+00:00",
          "updatedByUserId": "3ad2a24b-d349-4f8d-b38e-1ed8628da348"
        }
      }
      """,
    MediatedRequest.class);
  }
}
