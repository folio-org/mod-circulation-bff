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

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CheckOutApiTest extends BaseIT {

  private static final String CHECK_OUT_URL = "/circulation-bff/loans/check-out-by-barcode";
  private static final String CIRCULATION_CHECK_OUT_URL = "/circulation/check-out-by-barcode";
  private static final String TLR_CHECK_OUT_URL = "/tlr/loans/check-out-by-barcode";

  @Test
  @SneakyThrows
  void checkOutSuccess() {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(false);

    CheckOutRequest request = new CheckOutRequest()
      .itemBarcode("test_barcode")
      .userBarcode("user_barcode")
      .servicePointId(randomUUID());

    var mockResponse = new CheckOutResponse().id(UUID.randomUUID().toString());

    wireMockServer.stubFor(WireMock.post(urlMatching(CIRCULATION_CHECK_OUT_URL))
      .withRequestBody(equalToJson(asJsonString(request)))
      .willReturn(jsonResponse(asJsonString(mockResponse), SC_OK)));

    checkOut(request)
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(mockResponse)));
  }

  @Test
  @SneakyThrows
  void checkOutCallsModTlrOnCentralTenant() {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);

    CheckOutRequest request = new CheckOutRequest()
      .itemBarcode("test_barcode")
      .userBarcode("user_barcode")
      .servicePointId(randomUUID());

    var mockResponse = new CheckOutResponse().id(UUID.randomUUID().toString());

    wireMockServer.stubFor(WireMock.post(urlMatching(TLR_CHECK_OUT_URL))
      .withRequestBody(equalToJson(asJsonString(request)))
      .willReturn(jsonResponse(asJsonString(mockResponse), SC_OK)));

    checkOut(request)
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(mockResponse)));
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 422, 500})
  @SneakyThrows
  void circulationCheckOutErrorsAreForwarded(int responseStatus) {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(false);

    CheckOutRequest request = new CheckOutRequest()
      .itemBarcode("item_barcode")
      .userBarcode("user_barcode")
      .servicePointId(randomUUID());

    String responseBody = "Response status is " + responseStatus;

    wireMockServer.stubFor(WireMock.post(urlMatching(CIRCULATION_CHECK_OUT_URL))
      .withRequestBody(equalToJson(asJsonString(request)))
      .willReturn(aResponse().withStatus(responseStatus).withBody(responseBody)));

    checkOut(request)
      .andExpect(status().is(responseStatus))
      .andExpect(content().string(responseBody));
  }

  public UserTenantCollection buildUserTenant(String tenantId) {
    var userTenant = new UserTenant();
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    userTenant.setTenantId(tenantId);
    return new UserTenantCollection().addUserTenantsItem(userTenant);
  }

  @Test
  @SneakyThrows
  void checkOutFailsWhenItemBarcodeIsMissingInRequest() {
    CheckOutRequest request = new CheckOutRequest()
      .userBarcode("user_barcode")
      .servicePointId(randomUUID());

    checkOut(request)
      .andExpect(status().isBadRequest());
  }

  @Test
  @SneakyThrows
  void checkOutFailsWhenUserBarcodeIsMissingInRequest() {
    CheckOutRequest request = new CheckOutRequest()
      .itemBarcode("item_barcode")
      .servicePointId(randomUUID());

    checkOut(request)
      .andExpect(status().isBadRequest());
  }

  @Test
  @SneakyThrows
  void checkOutFailsWhenServicePointIdIsMissingInRequest() {
    CheckOutRequest request = new CheckOutRequest()
      .itemBarcode("item_barcode")
      .userBarcode("user_barcode");

    checkOut(request)
      .andExpect(status().isBadRequest());
  }

  @SneakyThrows
  private ResultActions checkOut(CheckOutRequest checkOutRequest) {
    return mockMvc.perform(post(CHECK_OUT_URL)
      .content(asJsonString(checkOutRequest))
      .headers(defaultHeaders()));
  }
}
