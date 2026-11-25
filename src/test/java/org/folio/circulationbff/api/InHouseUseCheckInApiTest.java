package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.json.JsonCompareMode.STRICT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.UUID;

import org.folio.circulationbff.domain.dto.CheckInRequest;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

import com.github.tomakehurst.wiremock.client.WireMock;

import lombok.SneakyThrows;

class InHouseUseCheckInApiTest extends BaseIT {

  private static final String CHECK_IN_URL = "/circulation-bff/loans/check-in-by-barcode";
  private static final String CIRCULATION_CHECK_IN_URL = "/circulation/check-in-by-barcode";
  private static final String REQUESTS_MEDIATED_CHECK_IN_URL = "/requests-mediated/loans/check-in-by-barcode";

  @ParameterizedTest
  @NullSource
  @ValueSource(booleans = {false, true})
  @SneakyThrows
  void forwardsFlagsInDataTenantAndPreservesInferredInHouseUse(Boolean inHouseUse) {
    givenDataTenant();
    var request = buildCheckInRequest()
      .inHouseUse(inHouseUse)
      .checkedOutItemInHouseUseOverride(true);
    var responseBody = """
      {
        "item": {
          "id": "item_id",
          "instanceId": "instance_id",
          "status": {"name": "Available"}
        },
        "inHouseUse": true
      }
      """;
    givenCirculationResponse(request, TENANT_ID_COLLEGE, 200, responseBody);

    checkIn(request, TENANT_ID_COLLEGE)
      .andExpect(status().isOk())
      .andExpect(content().json(responseBody, STRICT));
    verifyCirculationRequest(request, TENANT_ID_COLLEGE);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(booleans = false)
  @SneakyThrows
  void preservesCheckedOutItemConfirmationError(Boolean override) {
    givenDataTenant();
    var request = buildCheckInRequest()
      .inHouseUse(true)
      .checkedOutItemInHouseUseOverride(override);
    var responseBody = """
      {
        "errors": [{
          "message": "Item is checked out; confirm in-house use",
          "code": "CHECKED_OUT_ITEM_IN_HOUSE_USE_REQUIRES_CONFIRMATION",
          "parameters": [{"key": "itemBarcode", "value": "test_barcode"}],
          "overridableBlock": {
            "name": "checkedOutItemInHouseUseOverride",
            "missingPermissions": []
          }
        }],
        "totalRecords": 1
      }
      """;
    givenCirculationResponse(request, TENANT_ID_COLLEGE, 422, responseBody);

    checkIn(request, TENANT_ID_COLLEGE)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(content().string(responseBody));
    verifyCirculationRequest(request, TENANT_ID_COLLEGE);
  }

  @Test
  @SneakyThrows
  void preservesCompositeCheckInResponseAfterConfirmedOverride() {
    givenDataTenant();
    var request = buildCheckInRequest()
      .inHouseUse(true)
      .checkedOutItemInHouseUseOverride(true);
    var responseBody = """
      {
        "item": {
          "id": "item_id",
          "instanceId": "instance_id",
          "status": {"name": "Available"}
        },
        "loan": {
          "id": "loan_id",
          "action": "checkedin",
          "status": {"name": "Closed"},
          "borrower": {"firstName": "Test", "lastName": "Patron"},
          "item": {"id": "item_id", "instanceId": "instance_id"}
        },
        "staffSlipContext": {
          "item": {"barcode": "test_barcode", "status": "Available"},
          "request": {"requestId": "request_id"}
        },
        "inHouseUse": false,
        "subsequentCheckIns": [{
          "item": {
            "id": "item_id",
            "instanceId": "instance_id",
            "status": {"name": "Available"}
          },
          "staffSlipContext": {
            "item": {"barcode": "test_barcode", "status": "Available"},
            "request": {"requestId": "request_id"}
          },
          "inHouseUse": true
        }]
      }
      """;
    givenCirculationResponse(request, TENANT_ID_COLLEGE, 200, responseBody);

    checkIn(request, TENANT_ID_COLLEGE)
      .andExpect(status().isOk())
      .andExpect(content().json(responseBody, STRICT));
    verifyCirculationRequest(request, TENANT_ID_COLLEGE);
  }

  @Test
  @SneakyThrows
  void rejectsExplicitInHouseUseInCentralTenantBeforeForwarding() {
    givenCentralTenant();
    var request = buildCheckInRequest()
      .inHouseUse(true)
      .checkedOutItemInHouseUseOverride(true);

    checkIn(request, TENANT_ID_CONSORTIUM)
      .andExpect(status().isUnprocessableEntity())
      .andExpect(jsonPath("$.errors[0].code").value("IN_HOUSE_USE_NOT_SUPPORTED_IN_CENTRAL_TENANT"))
      .andExpect(jsonPath("$.errors[0].message").value("In-house use is not supported in the Central tenant"))
      .andExpect(jsonPath("$.total_records").value(1));

    wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(CIRCULATION_CHECK_IN_URL)));
    wireMockServer.verify(0, postRequestedFor(urlPathEqualTo(REQUESTS_MEDIATED_CHECK_IN_URL)));
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(booleans = false)
  @SneakyThrows
  void preservesStandardCheckInInCentralTenant(Boolean inHouseUse) {
    givenCentralTenant();
    mockHelper.mockEcsTlrSettings(true);
    wireMockServer.stubFor(WireMock.get(WireMock.urlPathEqualTo("/search/instances"))
      .willReturn(jsonResponse("{\"instances\": []}", 200)));
    var request = buildCheckInRequest().inHouseUse(inHouseUse);
    var responseBody = """
      {"item": {"id": "item_id", "instanceId": "instance_id"}}
      """;
    givenCirculationResponse(request, TENANT_ID_CONSORTIUM, 200, responseBody);

    checkIn(request, TENANT_ID_CONSORTIUM)
      .andExpect(status().isOk())
      .andExpect(content().json(responseBody, STRICT));
    verifyCirculationRequest(request, TENANT_ID_CONSORTIUM);
  }

  private void givenDataTenant() {
    mockHelper.mockUserTenants(new UserTenant()
      .tenantId(TENANT_ID_COLLEGE)
      .centralTenantId(TENANT_ID_CONSORTIUM), TENANT_ID_COLLEGE);
    mockHelper.mockEcsTlrCirculationSettings(true, TENANT_ID_COLLEGE);
  }

  private void givenCentralTenant() {
    mockHelper.mockUserTenants(new UserTenant()
      .tenantId(TENANT_ID_CONSORTIUM)
      .centralTenantId(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
  }

  private CheckInRequest buildCheckInRequest() {
    return new CheckInRequest()
      .itemBarcode("test_barcode")
      .checkInDate(new Date())
      .servicePointId(UUID.randomUUID());
  }

  private void givenCirculationResponse(CheckInRequest request, String tenantId,
    int responseStatus, String responseBody) {

    wireMockServer.stubFor(WireMock.post(urlPathEqualTo(CIRCULATION_CHECK_IN_URL))
      .withHeader(HEADER_TENANT, WireMock.equalTo(tenantId))
      .withRequestBody(equalToJson(asJsonString(request)))
      .willReturn(jsonResponse(responseBody, responseStatus)));
  }

  private void verifyCirculationRequest(CheckInRequest request, String tenantId) {
    wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(CIRCULATION_CHECK_IN_URL))
      .withHeader(HEADER_TENANT, WireMock.equalTo(tenantId))
      .withRequestBody(equalToJson(asJsonString(request))));
  }

  @SneakyThrows
  private ResultActions checkIn(CheckInRequest request, String tenantId) {
    return mockMvc.perform(post(CHECK_IN_URL)
      .headers(buildHeaders(tenantId))
      .content(asJsonString(request)));
  }
}
