package org.folio.circulationbff.api;

import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import org.folio.circulationbff.domain.dto.CheckOutRequest;
import org.folio.circulationbff.domain.dto.CheckOutResponse;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.folio.circulationbff.domain.dto.UserTenantCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CheckOutApiTest extends BaseIT {

  private static final String CHECK_OUT_URL = "/circulation-bff/loans/check-out-by-barcode";
  private static final String CIRCULATION_CHECK_OUT_URL = "/circulation/check-out-by-barcode";
  private static final String TLR_CHECK_OUT_URL = "/tlr/loans/check-out-by-barcode";
  private static final UUID SERVICE_POINT_ID = randomUUID();

  @Test
  @SneakyThrows
  void checkOutSuccess() {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(false);

    CheckOutRequest request = buildCheckOutRequest();
    var mockResponse = buildCheckOutResponse();

    wireMockServer.stubFor(WireMock.post(urlMatching(CIRCULATION_CHECK_OUT_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .withRequestBody(equalToJson(asJsonString(request)))
      .willReturn(jsonResponse(asJsonString(mockResponse), SC_OK)));

    checkOut(request, TENANT_ID_CONSORTIUM)
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(mockResponse)));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @SneakyThrows
  void checkOutCallsCorrectEndpointBasedOnTlrSetting(boolean enableTlr) {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(enableTlr);

    CheckOutRequest request = buildCheckOutRequest();
    var mockResponse = new CheckOutResponse().id(randomUUID().toString());

    String expectedUrl = enableTlr ? TLR_CHECK_OUT_URL : CIRCULATION_CHECK_OUT_URL;
    CheckOutRequest expectedForwarderCheckOutRequest = enableTlr
      ? buildCheckOutRequest()
      : request;

    wireMockServer.stubFor(WireMock.post(urlMatching(expectedUrl))
      .withRequestBody(equalToJson(asJsonString(expectedForwarderCheckOutRequest)))
      .willReturn(jsonResponse(asJsonString(mockResponse), SC_OK)));

    checkOut(request, TENANT_ID_CONSORTIUM)
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(mockResponse)));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @SneakyThrows
  void checkOutCallsModCirculationOnDataTenant(boolean enableTlr) {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_COLLEGE), TENANT_ID_COLLEGE);
    mockHelper.mockEcsTlrCirculationSettings(enableTlr, TENANT_ID_COLLEGE);

    CheckOutRequest request = buildCheckOutRequest();
    var mockResponse = buildCheckOutResponse();

    wireMockServer.stubFor(WireMock.post(urlMatching(CIRCULATION_CHECK_OUT_URL))
      .withRequestBody(equalToJson(asJsonString(request)))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(asJsonString(mockResponse), SC_OK)));

    checkOut(request, TENANT_ID_COLLEGE)
       .andExpect(status().isOk())
       .andExpect(content().json(asJsonString(mockResponse)));
  }

  @ParameterizedTest
  @ValueSource(strings = { TENANT_ID_CONSORTIUM, TENANT_ID_SECURE })
  @SneakyThrows
  void checkOutCallsModTlrInCentralAndSecureTenant(String targetTenantId) {
    mockHelper.mockUserTenants(buildUserTenant(targetTenantId), targetTenantId);
    mockTlrSettings(targetTenantId);

    CheckOutResponse mockTlrCheckOutResponse = buildCheckOutResponse();
    CheckOutRequest expectedTlrCheckOutRequest = buildCheckOutRequest();

    wireMockServer.stubFor(WireMock.post(urlMatching(TLR_CHECK_OUT_URL))
      .withRequestBody(equalToJson(asJsonString(expectedTlrCheckOutRequest)))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(asJsonString(mockTlrCheckOutResponse), SC_OK)));

    checkOut(buildCheckOutRequest(), targetTenantId)
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(mockTlrCheckOutResponse)));
  }

  private static void mockTlrSettings(String tenantId) {
    if (tenantId.equals(TENANT_ID_CONSORTIUM)) {
      mockHelper.mockEcsTlrSettings(true);
    } else {
      mockHelper.mockEcsTlrCirculationSettings(true, tenantId);
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 422, 500})
  @SneakyThrows
  void circulationCheckOutErrorsAreForwarded(int responseStatus) {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(false);

    CheckOutRequest request = buildCheckOutRequest();
    String responseBody = "Response status is " + responseStatus;

    wireMockServer.stubFor(WireMock.post(urlMatching(CIRCULATION_CHECK_OUT_URL))
      .withRequestBody(equalToJson(asJsonString(request)))
      .willReturn(aResponse().withStatus(responseStatus).withBody(responseBody)));

    checkOut(request, TENANT_ID_CONSORTIUM)
      .andExpect(status().is(responseStatus))
      .andExpect(content().string(responseBody));
  }

  private static UserTenantCollection buildUserTenant(String tenantId) {
    var userTenant = new UserTenant();
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    userTenant.setTenantId(tenantId);
    return new UserTenantCollection().addUserTenantsItem(userTenant);
  }

  @Test
  @SneakyThrows
  void checkOutFailsWhenItemBarcodeIsMissingInRequest() {
    CheckOutRequest request = buildCheckOutRequest().itemBarcode(null);
    checkOut(request, TENANT_ID_CONSORTIUM)
      .andExpect(status().isBadRequest());
  }

  @Test
  @SneakyThrows
  void checkOutFailsWhenUserBarcodeIsMissingInRequest() {
    CheckOutRequest request = buildCheckOutRequest().userBarcode(null);
    checkOut(request, TENANT_ID_CONSORTIUM)
      .andExpect(status().isBadRequest());
  }

  @Test
  @SneakyThrows
  void checkOutFailsWhenServicePointIdIsMissingInRequest() {
    CheckOutRequest request = buildCheckOutRequest().servicePointId(null);
    checkOut(request, TENANT_ID_CONSORTIUM)
      .andExpect(status().isBadRequest());
  }

  @SneakyThrows
  private ResultActions checkOut(CheckOutRequest checkOutRequest, String tenantId) {
    return mockMvc.perform(post(CHECK_OUT_URL)
      .content(asJsonString(checkOutRequest))
      .headers(buildHeaders(tenantId)));
  }

  private static CheckOutRequest buildCheckOutRequest() {
    return new CheckOutRequest()
      .itemBarcode("test_barcode")
      .userBarcode("user_barcode")
      .servicePointId(SERVICE_POINT_ID);
  }

  private static CheckOutResponse buildCheckOutResponse() {
    return new CheckOutResponse().id(UUID.randomUUID().toString());
  }
}
