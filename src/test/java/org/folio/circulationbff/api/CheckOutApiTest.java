package org.folio.circulationbff.api;

import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import org.folio.circulationbff.domain.dto.CheckOutRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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

  @Test
  @SneakyThrows
  void checkOutSuccess() {
    CheckOutRequest request = new CheckOutRequest()
      .itemBarcode("test_barcode")
      .userBarcode("user_barcode")
      .servicePointId(randomUUID());

    String mockResponse = """
      {
        "randomProperty": "randomValue"
      }
      """;

    wireMockServer.stubFor(WireMock.post(urlMatching(CIRCULATION_CHECK_OUT_URL))
      .withRequestBody(equalToJson(asJsonString(request)))
      .willReturn(jsonResponse(mockResponse, SC_OK)));

    checkOut(request)
      .andExpect(status().isOk())
      .andExpect(content().json(mockResponse));
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 422, 500})
  @SneakyThrows
  void circulationCheckOutErrorsAreForwarded(int responseStatus) {
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
