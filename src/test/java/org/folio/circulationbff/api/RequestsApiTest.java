package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;
import static org.folio.circulationbff.util.TestUtils.mockUserTenants;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.folio.circulationbff.domain.dto.BatchRequestDetail;
import org.folio.circulationbff.domain.dto.BatchRequestDetailsResponse;
import org.folio.circulationbff.domain.dto.BatchRequestResponse;
import org.folio.circulationbff.domain.dto.BffRequest;
import org.folio.circulationbff.domain.dto.EcsTlr;
import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.folio.circulationbff.domain.dto.MediatedRequests;
import org.folio.circulationbff.domain.dto.Request;
import org.folio.circulationbff.domain.dto.Requests;
import org.folio.circulationbff.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.github.tomakehurst.wiremock.client.WireMock;

import lombok.SneakyThrows;

class RequestsApiTest extends BaseIT {
  private static final String CIRCULATION_REQUEST_URL = "/circulation/requests";
  private static final String ECS_TLR_REQUEST_URL = "/tlr/ecs-tlr";
  private static final String REQUESTS_PATH = "/circulation-bff/requests";
  private static final String MEDIATED_BATCH_REQUEST_URL = "/requests-mediated/batch-mediated-requests";
  private static final String MEDIATED_BATCH_REQUEST_DETAILS_URL = "/requests-mediated/batch-mediated-requests/details";
  private static final String MEDIATED_REQUEST_URL = "/requests-mediated/mediated-requests";

  @MockitoBean
  private TenantService tenantService;

  @BeforeEach
  void beforeEach() {
    wireMockServer.resetAll();
  }

  @Test
  @SneakyThrows
  void createCirculationRequestInDataTenant() {
    mockUserTenants(wireMockServer, TENANT_ID_COLLEGE, UUID.randomUUID());
    mockHelper.mockEcsTlrCirculationSettings(true, TENANT_ID_COLLEGE);
    mockHelper.mockEcsTlrSettings(false);

    var request = new BffRequest()
      .requesterId(UUID.randomUUID().toString())
      .requestType(BffRequest.RequestTypeEnum.PAGE)
      .requestDate(new Date())
      .requestLevel(BffRequest.RequestLevelEnum.ITEM)
      .instanceId(UUID.randomUUID().toString())
      .fulfillmentPreference(BffRequest.FulfillmentPreferenceEnum.HOLD_SHELF)
      .pickupServicePointId(UUID.randomUUID().toString());

    wireMockServer.stubFor(WireMock.post(urlMatching(CIRCULATION_REQUEST_URL))
        .withRequestBody(equalToJson(asJsonString(request)))
      .willReturn(jsonResponse(asJsonString(new Request().id(UUID.randomUUID().toString())), SC_CREATED)));

    when(tenantService.isCentralTenant(anyString())).thenReturn(true);

    doPostWithTenant(REQUESTS_PATH, request, TENANT_ID_COLLEGE)
      .andExpect(MockMvcResultMatchers.status().isCreated())
      .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists());

    wireMockServer.verify(postRequestedFor(urlMatching(CIRCULATION_REQUEST_URL)));
  }

  @Test
  @SneakyThrows
  void createEcsTlrRequestInCentralTenant() {
    mockUserTenants(wireMockServer, TENANT_ID_CONSORTIUM, UUID.randomUUID());
    mockHelper.mockEcsTlrCirculationSettings(true, TENANT_ID_COLLEGE);
    mockHelper.mockEcsTlrSettings(true);

    var request = new BffRequest()
      .requesterId(UUID.randomUUID().toString())
      .requestType(BffRequest.RequestTypeEnum.PAGE)
      .requestDate(new Date())
      .requestLevel(BffRequest.RequestLevelEnum.TITLE)
      .instanceId(UUID.randomUUID().toString())
      .fulfillmentPreference(BffRequest.FulfillmentPreferenceEnum.HOLD_SHELF)
      .pickupServicePointId(UUID.randomUUID().toString());

    var primaryRequestId = UUID.randomUUID().toString();
    wireMockServer.stubFor(WireMock.post(urlMatching(ECS_TLR_REQUEST_URL))
      .willReturn(jsonResponse(asJsonString(
        new EcsTlr().id(UUID.randomUUID().toString())
          .primaryRequestId(primaryRequestId)), SC_CREATED)));

    wireMockServer.stubFor(WireMock.get(urlMatching(String.format("%s/%s",
        CIRCULATION_REQUEST_URL, primaryRequestId)))
      .willReturn(jsonResponse(asJsonString(
        new Request().id(UUID.randomUUID().toString())), SC_OK)));

    when(tenantService.isCentralTenant(anyString())).thenReturn(true);

    doPostWithTenant(REQUESTS_PATH, request, TENANT_ID_CONSORTIUM)
      .andExpect(MockMvcResultMatchers.status().isCreated())
      .andExpect(MockMvcResultMatchers.jsonPath("$.id").exists());

    wireMockServer.verify(postRequestedFor(urlMatching(ECS_TLR_REQUEST_URL)));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  @SneakyThrows
  void getRequestsWithBatchRequestInfo(boolean isSecureTenant) {
    // Given: mocks
    record BatchData(String batchId, Date submittedDate) {}

    // Create test data
    var batch1 = new BatchData(UUID.randomUUID().toString(), new Date());
    var batch2 = new BatchData(
      UUID.randomUUID().toString(),
      new Date(System.currentTimeMillis() - 86400000) // 1 day ago
    );

    var requestList = IntStream.range(0, 20)
      .mapToObj(i -> {
        var requestId = UUID.randomUUID().toString();
        return new Request()
          .id(requestId)
          .requestType(Request.RequestTypeEnum.PAGE)
          .status(Request.StatusEnum.OPEN_NOT_YET_FILLED)
          .requestLevel(Request.RequestLevelEnum.ITEM)
          .requesterId(UUID.randomUUID().toString())
          .itemId(UUID.randomUUID().toString())
          .instanceId(UUID.randomUUID().toString());
      })
      .toList();

    var requestsResponse = new Requests()
      .requests(requestList)
      .totalRecords(20);

    var mediatedRequestList = IntStream.range(0, 20)
      .mapToObj(i -> {
        var requestId = UUID.randomUUID().toString();
        return new MediatedRequest()
          .id(requestId)
          .requestType(MediatedRequest.RequestTypeEnum.PAGE)
          .status(MediatedRequest.StatusEnum.OPEN_NOT_YET_FILLED)
          .mediatedRequestStatus(MediatedRequest.MediatedRequestStatusEnum.OPEN)
          .requestLevel(MediatedRequest.RequestLevelEnum.ITEM)
          .requesterId(UUID.randomUUID().toString())
          .itemId(UUID.randomUUID().toString())
          .instanceId(UUID.randomUUID().toString())
          .confirmedRequestId(requestList.get(i).getId());
      })
      .toList();
    var mediatedRequestsResponse = new MediatedRequests()
      .mediatedRequests(mediatedRequestList)
      .totalRecords(20);

    // Mock circulation requests endpoint to return 40 requests
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(CIRCULATION_REQUEST_URL))
      .withQueryParam("query", equalTo("status==Open*"))
      .withQueryParam("limit", equalTo("20"))
      .withQueryParam("offset", equalTo("0"))
      .willReturn(jsonResponse(asJsonString(requestsResponse), SC_OK)));


    // Mock mediated batch request details endpoint
    var batchDetailsResponse = new BatchRequestDetailsResponse()
      .totalRecords(20);
    List<BatchRequestDetail> batchDetails;
    if (!isSecureTenant) {
      batchDetails = IntStream.range(0, 20)
        .mapToObj(i -> {
          var request = requestList.get(i);
          // Using ternary for simplicity, but demonstrating modern Java patterns
          var batchId = i < 10 ? batch1.batchId() : batch2.batchId();
          return new BatchRequestDetail()
            .confirmedRequestId(request.getId())
            .batchId(batchId);
        })
        .toList();

      when(tenantService.isCurrentTenantSecure()).thenReturn(false);
    } else {
      batchDetails = IntStream.range(0, 20)
        .mapToObj(i -> {
          var request = mediatedRequestList.get(i);
          // Using ternary for simplicity, but demonstrating modern Java patterns
          var batchId = i < 10 ? batch1.batchId() : batch2.batchId();
          return new BatchRequestDetail()
            .confirmedRequestId(request.getId())
            .batchId(batchId);
        })
        .toList();

      when(tenantService.isCurrentTenantSecure()).thenReturn(true);
      wireMockServer.stubFor(WireMock.get(urlPathEqualTo(MEDIATED_REQUEST_URL))
        .withQueryParam("query", matching("confirmedRequestId=.*"))
        .willReturn(jsonResponse(asJsonString(mediatedRequestsResponse), SC_OK)));
    }

    batchDetailsResponse.setMediatedBatchRequestDetails(batchDetails);
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(MEDIATED_BATCH_REQUEST_DETAILS_URL))
      .withQueryParam("query", matching("confirmedRequestId=.*"))
      .willReturn(jsonResponse(asJsonString(batchDetailsResponse), SC_OK)));

    var batchResponses = List.of(
      new BatchRequestResponse().batchId(batch1.batchId()).requestDate(batch1.submittedDate()),
      new BatchRequestResponse().batchId(batch2.batchId()).requestDate(batch2.submittedDate())
    );

    batchResponses.forEach(batchResponse ->
      wireMockServer.stubFor(WireMock.get(urlPathEqualTo(
          MEDIATED_BATCH_REQUEST_URL + "/" + batchResponse.getBatchId()))
        .willReturn(jsonResponse(asJsonString(batchResponse), SC_OK)))
    );

    // When: Make GET request to /circulation-bff/requests
    mockMvc.perform(get(REQUESTS_PATH)
        .queryParam("query", "status==Open*")
        .queryParam("limit", "20")
        .queryParam("offset", "0")
        .headers(defaultHeaders()))
      // Then: Verify response using method reference
      .andExpect(MockMvcResultMatchers.status().isOk())
      .andExpect(MockMvcResultMatchers.jsonPath("$.requests").isArray())
      .andExpect(MockMvcResultMatchers.jsonPath("$.requests.length()").value(20))
      .andExpect(MockMvcResultMatchers.jsonPath("$.totalRecords").value(20))
      .andExpect(MockMvcResultMatchers.jsonPath("$.requests[0].batchRequestInfo").exists())
      .andExpect(MockMvcResultMatchers.jsonPath("$.requests[0].batchRequestInfo.batchRequestId").exists())
      .andExpect(MockMvcResultMatchers.jsonPath("$.requests[0].batchRequestInfo.batchRequestSubmittedAt").exists())
      .andExpect(MockMvcResultMatchers.jsonPath("$.requests[19].batchRequestInfo").exists());

    // Verify external API calls were made
    wireMockServer.verify(getRequestedFor(urlPathEqualTo(CIRCULATION_REQUEST_URL))
      .withQueryParam("query", equalTo("status==Open*"))
      .withQueryParam("limit", equalTo("20"))
      .withQueryParam("offset", equalTo("0")));

    wireMockServer.verify(getRequestedFor(urlPathEqualTo(MEDIATED_BATCH_REQUEST_DETAILS_URL)));
  }

}
