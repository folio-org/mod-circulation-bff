package org.folio.circulationbff.api;

import static java.lang.String.format;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CirculationBffRequestsApiTest extends BaseIT {
  private static final String ALLOWED_SP_URL_PATH =
    "/circulation-bff/requests/allowed-service-points";
  private static final String ALLOWED_SP_QUERY_PARAM_TMP = "operation=%s&instanceId=%s&patronGroupId=%s";
  private static final String SEARCH_INSTANCES_URL_PATH =
    "/circulation-bff/requests/search-instances";
  private static final String SEARCH_INSTANCES_QUERY_PARAM_TMP = "query=%s";
  private static final String URL_TMP = "%s?%s";

  @Test
  void allowedServicePointsReturnsOkStatus() throws Exception {
    mockMvc.perform(
        get(buildUrl(ALLOWED_SP_URL_PATH, ALLOWED_SP_QUERY_PARAM_TMP, "create", randomId(),
          randomId()))
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());
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
}