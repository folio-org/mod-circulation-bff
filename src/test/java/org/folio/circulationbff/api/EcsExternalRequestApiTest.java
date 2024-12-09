package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.folio.circulationbff.domain.dto.EcsRequestExternal;
import org.folio.circulationbff.domain.dto.EcsTlr;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.folio.circulationbff.domain.dto.UserTenantCollection;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.github.tomakehurst.wiremock.client.WireMock;

import lombok.SneakyThrows;

class EcsExternalRequestApiTest extends BaseIT {
  private static final String TLR_CREATE_ECS_EXTERNAL_REQUEST_URL =
    "/tlr/create-ecs-request-external";
  private static final String CIRCULATION_BFF_CREATE_ECS_EXTERNAL_REQUEST_URL =
    "/circulation-bff/create-ecs-request-external";
  private static final String TEST_CENTRAL_TENANT_ID = "testCentralTenantId";

  @Test
  @SneakyThrows
  void postEcsRequestExternalTest() {
    EcsRequestExternal requestExternal = buildEcsRequestExternal();
    mockEcsTlrExternalRequestCreating(requestExternal);
    mockUserTenants();
    mockPerform(requestExternal);

    wireMockServer.verify(1, postRequestedFor(urlPathMatching(TLR_CREATE_ECS_EXTERNAL_REQUEST_URL))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TEST_CENTRAL_TENANT_ID)));
  }

  private static void mockUserTenants() {
    UserTenant userTenant = new UserTenant();
    userTenant.setCentralTenantId(TEST_CENTRAL_TENANT_ID);
    UserTenantCollection userTenants = new UserTenantCollection(List.of(userTenant), 1);

    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(USER_TENANTS_URL))
      .withQueryParam("limit", matching("\\d*"))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(asJsonString(userTenants), SC_OK)));
  }

  private static EcsRequestExternal buildEcsRequestExternal() {
    return new EcsRequestExternal(
      UUID.randomUUID().toString(),
      UUID.randomUUID().toString(),
      EcsRequestExternal.RequestLevelEnum.ITEM,
      new Date(LocalDate.of(2000, 1, 1).toEpochDay()),
      EcsRequestExternal.FulfillmentPreferenceEnum.HOLD_SHELF
    );
  }

  private void mockPerform(EcsRequestExternal requestExternal) throws Exception {
    mockMvc.perform(buildRequest(MockMvcRequestBuilders.post(
      CIRCULATION_BFF_CREATE_ECS_EXTERNAL_REQUEST_URL), requestExternal)
      .header(XOkapiHeaders.TENANT, TENANT_ID_CONSORTIUM));
  }

  private static void mockEcsTlrExternalRequestCreating(EcsRequestExternal requestExternal) {
    wireMockServer.stubFor(WireMock.post(urlMatching(TLR_CREATE_ECS_EXTERNAL_REQUEST_URL))
      .withRequestBody(equalToJson(asJsonString(requestExternal)))
      .willReturn(jsonResponse(asJsonString(new EcsTlr()), SC_CREATED)));
  }
}
