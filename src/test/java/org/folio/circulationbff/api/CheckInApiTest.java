package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;

import org.folio.circulationbff.domain.dto.CheckInRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

import com.github.tomakehurst.wiremock.client.WireMock;

import lombok.SneakyThrows;

class CheckInApiTest extends BaseIT {

  private static final String CHECK_IN_URL = "/circulation-bff/loans/check-in-by-barcode";
  private static final String CIRCULATION_CHECK_IN_URL = "/circulation/check-in-by-barcode";

  @Test
  @SneakyThrows
  void checkInSuccess() {
    CheckInRequest request = new CheckInRequest()
      .itemBarcode("test_barcode")
      .checkInDate(new Date())
      .servicePointId(randomUUID());

    String mockResponse = """
      {
        "randomProperty": "randomValue",
        "toServicePoint": "Test service point"
      }
      """;

    wireMockServer.stubFor(WireMock.post(urlMatching(CIRCULATION_CHECK_IN_URL))
      .withRequestBody(equalToJson(asJsonString(request)))
      .willReturn(jsonResponse(mockResponse, SC_OK)));

    checkIn(request)
      .andExpect(status().isOk())
      .andExpect(content().json(mockResponse));
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 422, 500})
  @SneakyThrows
  void circulationCheckInErrorsAreForwarded(int responseStatus) {
    CheckInRequest request = new CheckInRequest()
      .itemBarcode("test_barcode")
      .checkInDate(new Date())
      .servicePointId(randomUUID());

    String responseBody = "Response status is " + responseStatus;

    wireMockServer.stubFor(WireMock.post(urlMatching(CIRCULATION_CHECK_IN_URL))
      .withRequestBody(equalToJson(asJsonString(request)))
      .willReturn(aResponse().withStatus(responseStatus).withBody(responseBody)));

    checkIn(request)
      .andExpect(status().is(responseStatus))
      .andExpect(content().string(responseBody));
  }

  @Test
  @SneakyThrows
  void checkInFailsWhenItemBarcodeIsMissingInRequest() {
    CheckInRequest request = new CheckInRequest()
      .checkInDate(new Date())
      .servicePointId(randomUUID());

    checkIn(request)
      .andExpect(status().isBadRequest());
  }

  @Test
  @SneakyThrows
  void checkInFailsWhenCheckInDateIsMissingInRequest() {
    CheckInRequest request = new CheckInRequest()
      .itemBarcode("test_barcode")
      .servicePointId(randomUUID());

    checkIn(request)
      .andExpect(status().isBadRequest());
  }

  @Test
  @SneakyThrows
  void checkInFailsWhenServicePointIdIsMissingInRequest() {
    CheckInRequest request = new CheckInRequest()
      .itemBarcode("test_barcode")
      .checkInDate(new Date());

    checkIn(request)
      .andExpect(status().isBadRequest());
  }

  @SneakyThrows
  private ResultActions checkIn(CheckInRequest checkInRequest) {
    return mockMvc.perform(post(CHECK_IN_URL)
      .content(asJsonString(checkInRequest))
      .headers(defaultHeaders()));
  }
}
