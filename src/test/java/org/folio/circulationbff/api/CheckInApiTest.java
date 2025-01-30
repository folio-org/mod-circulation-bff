package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.List;

import org.folio.circulationbff.domain.dto.Campus;
import org.folio.circulationbff.domain.dto.CheckInRequest;
import org.folio.circulationbff.domain.dto.Institution;
import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.Library;
import org.folio.circulationbff.domain.dto.Location;
import org.folio.circulationbff.domain.dto.SearchInstance;
import org.folio.circulationbff.domain.dto.SearchInstances;
import org.folio.circulationbff.domain.dto.SearchItem;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.folio.circulationbff.domain.dto.UserTenantCollection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.ResultActions;

import com.github.tomakehurst.wiremock.client.WireMock;

import lombok.SneakyThrows;

class CheckInApiTest extends BaseIT {

  private static final String CHECK_IN_URL = "/circulation-bff/loans/check-in-by-barcode";
  private static final String CIRCULATION_CHECK_IN_URL = "/circulation/check-in-by-barcode";
  private static final String DCB_ITEM_ID = "9d1b77e8-f02e-4b7f-b296-3f2042ddac54";

  @Test
  @SneakyThrows
  void checkInSuccess() {
    var request = new CheckInRequest()
      .itemBarcode("test_barcode")
      .checkInDate(new Date())
      .servicePointId(randomUUID());
    givenCirculationCheckinSucceed(request);
    var checkinItem = new Item()
      .id(DCB_ITEM_ID)
      .copyNumber("copyNumber")
      .effectiveLocationId("effectiveLocationId");
    givenSearchInstanceReturnsItem(TENANT_ID_CONSORTIUM, checkinItem);
    givenCurrentTenantIsConsortium();
    wireMockServer.stubFor(WireMock.get(urlMatching("/item-storage/items/" + DCB_ITEM_ID))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(checkinItem, SC_OK)));

    var primaryServicePoint = randomUUID();
    var institutionId = randomUUID().toString();
    var campusId = randomUUID().toString();
    var libraryId = randomUUID().toString();
    var location = new Location()
      .primaryServicePoint(primaryServicePoint)
      .institutionId(institutionId)
      .campusId(campusId)
      .libraryId(libraryId);
    wireMockServer.stubFor(WireMock.get(urlMatching("/locations/effectiveLocationId"))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(location, SC_OK)));
    var servicePointResponse = """
      {
        "name": "updated service point",
        "holdShelfClosedLibraryDateManagement": "Keep_the_current_due_date"
      }
      """;
    var institution = new Institution().id(institutionId).name("institution");
    var campus = new Campus().id(campusId).name("campus");
    var library = new Library().id(libraryId).name("library");
    wireMockServer.stubFor(WireMock.get(urlMatching("/service-points/" + primaryServicePoint))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(servicePointResponse, SC_OK)));
    wireMockServer.stubFor(WireMock.get(urlMatching("/location-units/institutions/" + institutionId))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(institution, SC_OK)));
    wireMockServer.stubFor(WireMock.get(urlMatching("/location-units/campuses/" + campusId))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(campus, SC_OK)));
    wireMockServer.stubFor(WireMock.get(urlMatching("/location-units/libraries/" + libraryId))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(library, SC_OK)));

    var updatedServicePoint = "updated service point";
    checkIn(request)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.staffSlipContext.item.toServicePoint", equalTo(updatedServicePoint)))
      .andExpect(jsonPath("$.staffSlipContext.item.effectiveLocationPrimaryServicePointName", equalTo(updatedServicePoint)));
  }

  @Test
  @SneakyThrows
  void checkInSuccessCrossTenant() {
    var request = new CheckInRequest()
      .itemBarcode("test_barcode")
      .checkInDate(new Date())
      .servicePointId(randomUUID());
    givenCirculationCheckinSucceed(request);
    var checkinItem = new Item()
      .id(DCB_ITEM_ID)
      .copyNumber("copyNumber")
      .effectiveLocationId("effectiveLocationId");
    givenSearchInstanceReturnsItem(TENANT_ID_COLLEGE, checkinItem);
    givenCurrentTenantIsConsortium();
    wireMockServer.stubFor(WireMock.get(urlMatching("/item-storage/items/" + DCB_ITEM_ID))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(checkinItem, SC_OK)));

    var primaryServicePoint = randomUUID();
    var institutionId = randomUUID().toString();
    var campusId = randomUUID().toString();
    var libraryId = randomUUID().toString();
    var location = new Location()
      .primaryServicePoint(primaryServicePoint)
      .institutionId(institutionId)
      .campusId(campusId)
      .libraryId(libraryId);
    wireMockServer.stubFor(WireMock.get(urlMatching("/locations/effectiveLocationId"))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(location, SC_OK)));
    var servicePointResponse = """
      {
        "name": "updated service point",
        "holdShelfClosedLibraryDateManagement": "Keep_the_current_due_date"
      }
      """;
    var institution = new Institution().id(institutionId).name("institution");
    var campus = new Campus().id(campusId).name("campus");
    var library = new Library().id(libraryId).name("library");
    wireMockServer.stubFor(WireMock.get(urlMatching("/service-points/" + primaryServicePoint))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(servicePointResponse, SC_OK)));
    wireMockServer.stubFor(WireMock.get(urlMatching("/location-units/institutions/" + institutionId))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(institution, SC_OK)));
    wireMockServer.stubFor(WireMock.get(urlMatching("/location-units/campuses/" + campusId))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(campus, SC_OK)));
    wireMockServer.stubFor(WireMock.get(urlMatching("/location-units/libraries/" + libraryId))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(library, SC_OK)));

    var updatedServicePoint = "updated service point";
    checkIn(request)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.staffSlipContext.item.toServicePoint", equalTo(updatedServicePoint)))
      .andExpect(jsonPath("$.staffSlipContext.item.effectiveLocationPrimaryServicePointName", equalTo(updatedServicePoint)));
  }

  @Test
  @SneakyThrows
  void checkInSuccessWhenInstanceNotFound() {
    var request = new CheckInRequest()
      .itemBarcode("test_barcode")
      .checkInDate(new Date())
      .servicePointId(randomUUID());
    givenCirculationCheckinSucceed(request);
    var searchInstances = new SearchInstances().instances(List.of());
    wireMockServer.stubFor(WireMock.get(urlMatching("/search/instances.*"))
      .willReturn(jsonResponse(searchInstances, SC_OK)));

    checkIn(request)
      .andExpect(status().isOk());
  }

  private void givenCirculationCheckinSucceed(CheckInRequest request) {
    var checkinResponse = String.format("""
      {
        "item": {"id": "%s"},
        "staffSlipContext": {
          "item": {
            "toServicePoint": "random service point",
            "effectiveLocationPrimaryServicePointName": "random service point"
          }
        }
      }
      """, DCB_ITEM_ID);
    wireMockServer.stubFor(WireMock.post(urlMatching(CIRCULATION_CHECK_IN_URL))
      .withRequestBody(equalToJson(asJsonString(request)))
      .willReturn(jsonResponse(checkinResponse, SC_OK)));
  }

  private void givenSearchInstanceReturnsItem(String tenantId, Item item) {
    var searchItem = new SearchItem()
      .id(item.getId())
      .tenantId(tenantId);
    var searchInstance = new SearchInstance()
      .tenantId(tenantId)
      .items(List.of(searchItem));
    var searchResponse = new SearchInstances().instances(List.of(searchInstance));
    wireMockServer.stubFor(WireMock.get(urlMatching("/search/instances.*"))
      .willReturn(jsonResponse(searchResponse, SC_OK)));
  }

  private void givenCurrentTenantIsConsortium() {
    var tenantsResponse = new UserTenantCollection()
      .userTenants(List.of(new UserTenant().tenantId(TENANT_ID_CONSORTIUM)));
    wireMockServer.stubFor(WireMock.get(urlMatching("/user-tenants.*"))
      .willReturn(jsonResponse(tenantsResponse, SC_OK)));
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
