package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.folio.circulationbff.util.MockHelper.MEDIATED_BATCH_REQUEST_URL;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import lombok.SneakyThrows;
import org.folio.circulationbff.util.MockHelper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;


class CirculationBffBatchRequestsApiTest extends BaseIT {

  private static final String BASE_PATH = "/circulation-bff/batch-requests";

  @Test
  @SneakyThrows
  void createBatchRequestReturnsCreated() {
    var batchRequest = MockHelper.buildBatchRequest();
    var batchResponse = MockHelper.buildBatchRequestResponse();
    mockHelper.mockCreateBatchRequest(batchResponse);

    mockMvc.perform(post(BASE_PATH)
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(batchRequest))
        .headers(defaultHeaders()))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.batchId", is(batchResponse.getBatchId())));

    wireMockServer.verify(postRequestedFor(urlPathEqualTo(MEDIATED_BATCH_REQUEST_URL)));
  }

  @Test
  @SneakyThrows
  void getBatchRequestByIdReturnsOk() {
    var batchId = UUID.randomUUID().toString();
    var batchResponse = MockHelper.buildBatchRequestResponse();
    mockHelper.mockGetBatchRequestById(batchId, batchResponse);

    mockMvc.perform(get(BASE_PATH + "/" + batchId)
        .headers(defaultHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.batchId", is(batchResponse.getBatchId())));

    wireMockServer.verify(getRequestedFor(urlPathEqualTo(MEDIATED_BATCH_REQUEST_URL + "/" + batchId)));
  }

  @Test
  @SneakyThrows
  void getBatchRequestCollectionReturnsOk() {
    var query = "status==Completed";
    var offset = "0";
    var limit = "10";
    var collectionResponse = MockHelper.buildBatchRequestCollectionResponse();
    mockHelper.mockGetBatchRequestCollection(query, offset, limit, collectionResponse);

    mockMvc.perform(get(BASE_PATH)
        .queryParam("query", query)
        .queryParam("offset", offset)
        .queryParam("limit", limit)
        .headers(defaultHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalRecords", is(collectionResponse.getTotalRecords())));

    wireMockServer.verify(getRequestedFor(urlPathEqualTo(MEDIATED_BATCH_REQUEST_URL))
      .withQueryParam("query", equalTo(query))
      .withQueryParam("offset", equalTo(offset))
      .withQueryParam("limit", equalTo(limit)));
  }

  @Test
  @SneakyThrows
  void getMultiItemBatchRequestDetailsByBatchIdReturnsOk() {
    var batchId = UUID.randomUUID().toString();
    var offset = "0";
    var limit = "5";
    var detailsResponse = MockHelper.buildBatchRequestDetailsResponse();
    mockHelper.mockGetMultiItemBatchRequestDetails(batchId, offset, limit, detailsResponse);

    mockMvc.perform(get(BASE_PATH + "/" + batchId + "/details")
        .queryParam("offset", offset)
        .queryParam("limit", limit)
        .headers(defaultHeaders()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.mediatedBatchRequestDetails[0].itemId",
        is(detailsResponse.getMediatedBatchRequestDetails().getFirst().getItemId())));

    wireMockServer.verify(getRequestedFor(urlPathEqualTo(MEDIATED_BATCH_REQUEST_URL + "/" + batchId + "/details"))
      .withQueryParam("offset", equalTo(offset))
      .withQueryParam("limit", equalTo(limit)));
  }
}
