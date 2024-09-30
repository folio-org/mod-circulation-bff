package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.http.HttpStatus;
import org.folio.circulationbff.domain.dto.Instance;
import org.folio.circulationbff.domain.dto.InstanceSearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.github.tomakehurst.wiremock.client.WireMock;
class CirculationBffRequestsApiTest extends BaseIT {
  private static final String ALLOWED_SP_URL_PATH =
    "/circulation-bff/requests/allowed-service-points";
  private static final String ALLOWED_SP_QUERY_PARAM_TMP = "operation=%s&instanceId=%s&patronGroupId=%s";
  private static final String SEARCH_INSTANCES_URL_PATH =
    "/circulation-bff/requests/search-instances";
  private static final String SEARCH_INSTANCES_MOD_SEARCH_URL_PATH = "/search/instances";
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
    String instanceId = randomId();
    InstanceSearchResult mockSearchResponse = new InstanceSearchResult()
      .addInstancesItem(new Instance().id(instanceId))
      .totalRecords(1);

    wireMockServer.stubFor(WireMock.get(urlPathMatching(SEARCH_INSTANCES_MOD_SEARCH_URL_PATH))
      .withQueryParam("query", equalTo("id==" + instanceId))
      .withQueryParam("expandAll", equalTo("true"))
      .willReturn(jsonResponse(mockSearchResponse, HttpStatus.SC_OK)));

    mockMvc.perform(
        get(SEARCH_INSTANCES_URL_PATH)
          .queryParam("query", "id==" + instanceId)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("instances[0].id", is(instanceId)))
      .andExpect(jsonPath("totalRecords", is(1)));
  }

  private String buildUrl(String path, String parametersTemplate, String... parameters) {
    return format(URL_TMP, path, format(parametersTemplate, (Object[]) parameters));
  }
}