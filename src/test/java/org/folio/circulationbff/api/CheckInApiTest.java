package org.folio.circulationbff.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.folio.circulationbff.domain.dto.CheckInRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import lombok.SneakyThrows;

public class CheckInApiTest extends BaseIT {

  private static final String CHECK_IN_URL = "/circulation-bff/check-in-by-barcode";

  @Test
  @SneakyThrows
  void checkInSuccess() {
    checkIn(new CheckInRequest().itemBarcode("test_barcode"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.itemBarcode", Matchers.is("test_barcode")));
  }

  @SneakyThrows
  private ResultActions checkIn(CheckInRequest checkInRequest) {
    return mockMvc.perform(post(CHECK_IN_URL)
      .content(asJsonString(checkInRequest))
      .headers(defaultHeaders())
      .contentType(MediaType.APPLICATION_JSON));
  }
}
