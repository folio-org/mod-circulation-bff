package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.lang.String.format;
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
import org.folio.circulationbff.domain.dto.Contributor;
import org.folio.circulationbff.domain.dto.HoldingsRecord;
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
  private static final String CIRCULATION_ITEM_URL = "/circulation-item/%s";
  private static final String HOLDING_STORAGE_URL = "/holdings-storage/holdings/%s";
  private static final String DCB_INSTANCE_ID = "9d1b77e4-f02e-4b7f-b296-3f2042ddac54";
  private static final String INSTANCE_ID = "4bd52525-b922-4b20-9b3b-caf7b2d1866f";

  @Test
  @SneakyThrows
  void checkInSameTenantSuccessForDcbItem() {
    mockHelper.mockEcsTlrSettings(true);
    var request = buildCheckInRequest();
    var itemId = randomId();
    var effectiveLocationId = randomId();
    givenCirculationCheckInSucceeded(request, itemId, DCB_INSTANCE_ID);
    var checkinItem = new Item()
      .id(itemId)
      .copyNumber("copyNumber")
      .effectiveLocationId(effectiveLocationId)
      .itemLevelCallNumber("CN");
    givenSearchInstanceReturnsItem(TENANT_ID_CONSORTIUM, checkinItem);
    givenCurrentTenantIsConsortium();
    wireMockServer.stubFor(WireMock.get(urlMatching("/item-storage/items/" + itemId))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(checkinItem, SC_OK)));

    var primaryServicePoint = randomUUID();
    var institutionId = randomId();
    var campusId = randomId();
    var libraryId = randomId();
    var location = new Location()
      .primaryServicePoint(primaryServicePoint)
      .institutionId(institutionId)
      .campusId(campusId)
      .libraryId(libraryId);
    wireMockServer.stubFor(WireMock.get(urlMatching("/locations/" + effectiveLocationId))
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
  void checkInCrossTenantSuccessForDcbItem() {
    mockHelper.mockEcsTlrSettings(true);
    var request = buildCheckInRequest();
    var effectiveLocationId = randomId();
    var itemId = randomId();
    var primaryServicePointId = randomUUID();
    var primaryServicePointName = "updated service point";
    var holdingRecordId = randomId();
    var callNumber = "CNTST";
    givenCirculationCheckInForInTransitItemSucceed(request, itemId, DCB_INSTANCE_ID);
    var checkinItem = new Item()
      .id(itemId)
      .holdingsRecordId(holdingRecordId)
      .inTransitDestinationServicePointId(primaryServicePointId.toString())
      .copyNumber("copyNumber")
      .effectiveLocationId(effectiveLocationId)
      .itemLevelCallNumber(callNumber);
    givenSearchInstanceReturnsItem(TENANT_ID_COLLEGE, checkinItem);
    givenCurrentTenantIsConsortium();
    wireMockServer.stubFor(WireMock.get(urlMatching("/item-storage/items/" + itemId))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(checkinItem, SC_OK)));

    var institutionId = randomId();
    var campusId = randomId();
    var libraryId = randomId();
    var location = new Location()
      .name("location")
      .primaryServicePoint(primaryServicePointId)
      .institutionId(institutionId)
      .campusId(campusId)
      .libraryId(libraryId);
    wireMockServer.stubFor(WireMock.get(urlMatching("/locations/" + effectiveLocationId))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(location, SC_OK)));
    var servicePointResponse = format("""
      {
        "name": "%s",
        "id": "%s",
        "holdShelfClosedLibraryDateManagement": "Keep_the_current_due_date"
      }
      """, primaryServicePointName, primaryServicePointId);
    var institution = new Institution().id(institutionId).name("institution");
    var campus = new Campus().id(campusId).name("campus");
    var library = new Library().id(libraryId).name("library");
    wireMockServer.stubFor(WireMock.get(urlMatching("/service-points/" + primaryServicePointId))
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

    checkIn(request)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.staffSlipContext.item.toServicePoint", equalTo(primaryServicePointName)))
      .andExpect(jsonPath("$.staffSlipContext.item.effectiveLocationPrimaryServicePointName", equalTo(primaryServicePointName)))
      .andExpect(jsonPath("$.staffSlipContext.item.effectiveLocationInstitution", equalTo(institution.getName())))
      .andExpect(jsonPath("$.staffSlipContext.item.effectiveLocationCampus", equalTo(campus.getName())))
      .andExpect(jsonPath("$.staffSlipContext.item.effectiveLocationLibrary", equalTo(library.getName())))
      .andExpect(jsonPath("$.staffSlipContext.item.effectiveLocationSpecific", equalTo(location.getName())))
      .andExpect(jsonPath("$.staffSlipContext.item.callNumber", equalTo(callNumber)))
      .andExpect(jsonPath("$.item.inTransitDestinationServicePointId", equalTo(primaryServicePointId.toString())))
      .andExpect(jsonPath("$.item.inTransitDestinationServicePoint.id", equalTo(primaryServicePointId.toString())))
      .andExpect(jsonPath("$.item.inTransitDestinationServicePoint.name", equalTo(primaryServicePointName)))
      .andExpect(jsonPath("$.item.location.name", equalTo(location.getName())))
      .andExpect(jsonPath("$.item.holdingsRecordId", equalTo(checkinItem.getHoldingsRecordId())))
      .andExpect(jsonPath("$.item.instanceId", equalTo((INSTANCE_ID))));
  }

  @Test
  @SneakyThrows
  void checkInCrossTenantCallNumberFromHolding() {
    mockHelper.mockEcsTlrSettings(true);
    var request = buildCheckInRequest();
    var effectiveLocationId = randomId();
    var itemId = randomId();
    var primaryServicePointId = randomUUID();
    var primaryServicePointName = "updated service point";
    var holdingRecordId = randomId();
    givenCirculationCheckInForInTransitItemSucceed(request, itemId, DCB_INSTANCE_ID);
    var checkinItem = new Item()
      .id(itemId)
      .holdingsRecordId(holdingRecordId)
      .inTransitDestinationServicePointId(primaryServicePointId.toString())
      .effectiveLocationId(effectiveLocationId);
    givenSearchInstanceReturnsItem(TENANT_ID_COLLEGE, checkinItem);
    givenCurrentTenantIsConsortium();
    wireMockServer.stubFor(WireMock.get(urlMatching("/item-storage/items/" + itemId))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(checkinItem, SC_OK)));

    var institutionId = randomId();
    var campusId = randomId();
    var libraryId = randomId();
    var location = new Location()
      .name("location")
      .primaryServicePoint(primaryServicePointId)
      .institutionId(institutionId)
      .campusId(campusId)
      .libraryId(libraryId);
    wireMockServer.stubFor(WireMock.get(urlMatching("/locations/" + effectiveLocationId))
            .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_COLLEGE))
            .willReturn(jsonResponse(location, SC_OK)));

    var servicePointResponse = format("""
      {
        "name": "%s",
        "id": "%s",
        "holdShelfClosedLibraryDateManagement": "Keep_the_current_due_date"
      }
      """, primaryServicePointName, primaryServicePointId);

    var institution = new Institution().id(institutionId).name("institution");
    var campus = new Campus().id(campusId).name("campus");
    var library = new Library().id(libraryId).name("library");
    wireMockServer.stubFor(WireMock.get(urlMatching("/service-points/" + primaryServicePointId))
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

    var cn = "CN-TST-HOLDING";
    var cnPrefix = "PRFX";
    var cnSuffix = "SFX";

    var holdingRecordResponse = new HoldingsRecord()
            .id(holdingRecordId)
            .callNumber(cn)
            .callNumberPrefix(cnPrefix)
            .callNumberSuffix(cnSuffix);

    wireMockServer.stubFor(WireMock.get(urlMatching(format(HOLDING_STORAGE_URL, holdingRecordId)))
            .willReturn(jsonResponse(asJsonString(holdingRecordResponse), SC_OK)));

    checkIn(request).andExpect(status().isOk())
      .andExpect(jsonPath("$.item.holdingsRecordId", equalTo(checkinItem.getHoldingsRecordId())))
      .andExpect(jsonPath("$.item.id", equalTo((itemId))))
      .andExpect(jsonPath("$.staffSlipContext.item.callNumber", equalTo((cn))))
      .andExpect(jsonPath("$.staffSlipContext.item.callNumberPrefix", equalTo((cnPrefix))))
      .andExpect(jsonPath("$.staffSlipContext.item.callNumberSuffix", equalTo((cnSuffix))));
  }

  @Test
  @SneakyThrows
  void checkInCrossTenantSuccessForDcbItemWithLoan() {
    mockHelper.mockEcsTlrSettings(true);
    var request = buildCheckInRequest();
    var effectiveLocationId = randomId();
    var itemId = randomId();
    var primaryServicePointId = randomUUID();
    var primaryServicePointName = "updated service point";
    var holdingRecordId = randomId();
    givenCirculationCheckInWithLoanSucceed(request, itemId, DCB_INSTANCE_ID);
    var checkinItem = new Item()
      .id(itemId)
      .holdingsRecordId(holdingRecordId)
      .inTransitDestinationServicePointId(primaryServicePointId.toString())
      .copyNumber("copyNumber")
      .effectiveLocationId(effectiveLocationId)
      .itemLevelCallNumber("CN");
    givenSearchInstanceReturnsItem(TENANT_ID_COLLEGE, checkinItem);
    givenCurrentTenantIsConsortium();
    wireMockServer.stubFor(WireMock.get(urlMatching("/item-storage/items/" + itemId))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(checkinItem, SC_OK)));

    var institutionId = randomId();
    var campusId = randomId();
    var libraryId = randomId();
    var location = new Location()
      .name("location")
      .primaryServicePoint(primaryServicePointId)
      .institutionId(institutionId)
      .campusId(campusId)
      .libraryId(libraryId);
    wireMockServer.stubFor(WireMock.get(urlMatching("/locations/" + effectiveLocationId))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(location, SC_OK)));
    var servicePointResponse = format("""
      {
        "name": "%s",
        "id": "%s",
        "holdShelfClosedLibraryDateManagement": "Keep_the_current_due_date"
      }
      """, primaryServicePointName, primaryServicePointId);
    var institution = new Institution().id(institutionId).name("institution");
    var campus = new Campus().id(campusId).name("campus");
    var library = new Library().id(libraryId).name("library");
    wireMockServer.stubFor(WireMock.get(urlMatching("/service-points/" + primaryServicePointId))
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

    checkIn(request)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.staffSlipContext.item.toServicePoint", equalTo(primaryServicePointName)))
      .andExpect(jsonPath("$.staffSlipContext.item.effectiveLocationPrimaryServicePointName", equalTo(primaryServicePointName)))
      .andExpect(jsonPath("$.staffSlipContext.item.effectiveLocationInstitution", equalTo(institution.getName())))
      .andExpect(jsonPath("$.staffSlipContext.item.effectiveLocationCampus", equalTo(campus.getName())))
      .andExpect(jsonPath("$.staffSlipContext.item.effectiveLocationLibrary", equalTo(library.getName())))
      .andExpect(jsonPath("$.staffSlipContext.item.effectiveLocationSpecific", equalTo(location.getName())))
      .andExpect(jsonPath("$.loan.item.inTransitDestinationServicePointId", equalTo(primaryServicePointId.toString())))
      .andExpect(jsonPath("$.loan.item.inTransitDestinationServicePoint.id", equalTo(primaryServicePointId.toString())))
      .andExpect(jsonPath("$.loan.item.inTransitDestinationServicePoint.name", equalTo(primaryServicePointName)))
      .andExpect(jsonPath("$.loan.item.location.name", equalTo(location.getName())))
      .andExpect(jsonPath("$.loan.item.holdingsRecordId", equalTo(checkinItem.getHoldingsRecordId())))
      .andExpect(jsonPath("$.loan.item.instanceId", equalTo((INSTANCE_ID))));
  }

  @Test
  @SneakyThrows
  void checkInSuccessWhenInstanceNotFound() {
    mockHelper.mockEcsTlrSettings(true);
    givenCurrentTenantIsConsortium();

    var request = buildCheckInRequest();
    givenCirculationCheckInSucceeded(request, randomId(), DCB_INSTANCE_ID);
    var searchInstances = new SearchInstances().instances(List.of());
    wireMockServer.stubFor(WireMock.get(urlMatching("/search/instances.*"))
      .willReturn(jsonResponse(searchInstances, SC_OK)));

    checkIn(request).andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  void checkInSuccessWhenItemNotFound() {
    mockHelper.mockEcsTlrSettings(true);
    var request = buildCheckInRequest();
    var effectiveLocationId = randomId();
    var itemId = randomId();
    givenCirculationCheckInSucceeded(request, itemId, DCB_INSTANCE_ID);
    var checkinItem = new Item()
      .id(itemId)
      .copyNumber("copyNumber")
      .effectiveLocationId(effectiveLocationId);
    givenSearchInstanceReturnsItem(TENANT_ID_COLLEGE, checkinItem);
    givenCurrentTenantIsConsortium();
    wireMockServer.stubFor(WireMock.get(urlMatching("/item-storage/items/" + itemId))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(null, SC_OK)));

    checkIn(request).andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  void checkInSuccessForNotDcbItem() {
    mockHelper.mockEcsTlrSettings(true);
    givenCurrentTenantIsConsortium();
    var request = buildCheckInRequest();
    givenCirculationCheckInSucceeded(request, randomId(), randomId());
    var searchInstances = new SearchInstances().instances(List.of());
    wireMockServer.stubFor(WireMock.get(urlMatching("/search/instances.*"))
      .willReturn(jsonResponse(searchInstances, SC_OK)));

    checkIn(request).andExpect(status().isOk());
  }

  private CheckInRequest buildCheckInRequest() {
    return new CheckInRequest()
      .itemBarcode("test_barcode")
      .checkInDate(new Date())
      .servicePointId(randomUUID());
  }

  private void givenCirculationCheckInSucceeded(CheckInRequest request, String itemId, String instanceId) {
    var checkinResponse = format("""
        {
          "item": {
            "id": "%s",
            "instanceId": "%s"
          },
          "staffSlipContext": {
            "item": {
              "toServicePoint": "random service point",
              "effectiveLocationPrimaryServicePointName": "random service point"
            }
          }
        }
        """, itemId, instanceId);
    wireMockServer.stubFor(WireMock.post(urlMatching(CIRCULATION_CHECK_IN_URL))
      .withRequestBody(equalToJson(asJsonString(request)))
      .willReturn(jsonResponse(checkinResponse, SC_OK)));
  }

  private void givenCirculationCheckInForInTransitItemSucceed(CheckInRequest request,
    String itemId, String instanceId) {

    var checkinResponse = format("""
        {
          "item": {
            "id": "%s",
            "instanceId": "%s",
            "holdingsRecordId": "DCB",
            "inTransitDestinationServicePointId": "DCB",
            "inTransitDestinationServicePoint": {
              "inTransitDestinationServicePointId": "DCB",
              "inTransitDestinationServicePointName": "DCB SP name"
            },
            "location": {
              "name": "DCB location"
            }
          },
          "staffSlipContext": {
            "item": {
              "toServicePoint": "random service point",
              "effectiveLocationPrimaryServicePointName": "random service point"
            }
          }
        }
        """, itemId, instanceId);
    wireMockServer.stubFor(WireMock.post(urlMatching(CIRCULATION_CHECK_IN_URL))
      .withRequestBody(equalToJson(asJsonString(request)))
      .willReturn(jsonResponse(checkinResponse, SC_OK)));
  }

  private void givenCirculationCheckInWithLoanSucceed(CheckInRequest request, String itemId,
    String instanceId) {

    var checkinResponse = format("""
      {
        "item": {
          "id": "%s",
          "instanceId": "%s",
          "holdingsRecordId": "DCB"
        },
        "loan": {
          "id": "%s",
          "item": {
            "inTransitDestinationServicePointId": "DCB",
            "inTransitDestinationServicePoint": {
              "inTransitDestinationServicePointId": "DCB",
              "inTransitDestinationServicePointName": "DCB SP name"
            },
            "location": {
              "name": "DCB"
            },
            "holdingsRecordId": "DCB",
            "instanceId": "%s"
          }
        },
        "staffSlipContext": {
          "item": {
            "toServicePoint": "random service point",
            "effectiveLocationPrimaryServicePointName": "random service point"
          }
        }
      }
      """, itemId, instanceId, randomId(), INSTANCE_ID);
    wireMockServer.stubFor(WireMock.post(urlMatching(CIRCULATION_CHECK_IN_URL))
      .withRequestBody(equalToJson(asJsonString(request)))
      .willReturn(jsonResponse(checkinResponse, SC_OK)));
  }

  private void givenSearchInstanceReturnsItem(String tenantId, Item item) {
    givenSearchInstanceReturnsItem(tenantId, new SearchItem().id(item.getId()));
  }

  private void givenSearchInstanceReturnsItem(String tenantId, SearchItem item) {
    item.tenantId(tenantId);

    var searchInstance = new SearchInstance()
      .id(INSTANCE_ID)
      .tenantId(tenantId)
      .contributors(List.of(
        new Contributor()
          .name("Test contributor")
          .primary(true)))
      .items(List.of(item));
    var searchResponse = new SearchInstances().instances(List.of(searchInstance));
    wireMockServer.stubFor(WireMock.get(urlMatching("/search/instances.*"))
      .willReturn(jsonResponse(searchResponse, SC_OK)));
  }

  private void givenCurrentTenantIsConsortium() {
    var tenantsResponse = new UserTenantCollection()
      .userTenants(List.of(new UserTenant()
        .tenantId(TENANT_ID_CONSORTIUM)
        .centralTenantId(TENANT_ID_CONSORTIUM)
      ));
    wireMockServer.stubFor(WireMock.get(urlMatching("/user-tenants.*"))
      .willReturn(jsonResponse(tenantsResponse, SC_OK)));
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 422, 500})
  @SneakyThrows
  void circulationCheckInErrorsAreForwarded(int responseStatus) {
    mockHelper.mockEcsTlrSettings(false);
    givenCurrentTenantIsConsortium();

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

  @Test
  @SneakyThrows
  void localCheckInWhenCannotFindItemBarcodeInSearchInstance() {
    mockHelper.mockEcsTlrSettings(true);
    givenCurrentTenantIsConsortium();
    var checkInRequest = buildCheckInRequest();
    givenCirculationCheckInSucceeded(checkInRequest, randomId(), randomId());
    var itemId = randomId();
    givenSearchInstanceReturnsItem(TENANT_ID_COLLEGE, new Item().id(itemId));

    checkIn(checkInRequest).andExpect(status().isOk());

    // Zero remote check ins
    wireMockServer.verify(0, postRequestedFor(urlPathMatching(CIRCULATION_CHECK_IN_URL))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_COLLEGE)));

    // One local check in (Central tenant)
    wireMockServer.verify(1, postRequestedFor(urlPathMatching(CIRCULATION_CHECK_IN_URL))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_CONSORTIUM))
      .withRequestBody(equalToJson(asJsonString(checkInRequest))));
  }

  @Test
  @SneakyThrows
  void localCheckInWhenCirculationItemExists() {
    mockHelper.mockEcsTlrSettings(true);
    givenCurrentTenantIsConsortium();
    var checkInRequest = buildCheckInRequest();
    givenCirculationCheckInSucceeded(checkInRequest, randomId(), randomId());
    var itemId = randomId();
    givenSearchInstanceReturnsItem(TENANT_ID_COLLEGE,
      new SearchItem().id(itemId).barcode("test_barcode"));

    var circulationItemResponse = format("""
      {
        "id": "%s"
      }
      """, itemId);
    wireMockServer.stubFor(WireMock.get(urlMatching(format(CIRCULATION_ITEM_URL, itemId)))
      .willReturn(jsonResponse(circulationItemResponse, SC_OK)));

    checkIn(checkInRequest).andExpect(status().isOk());

    // Zero remote check ins
    wireMockServer.verify(0, postRequestedFor(urlPathMatching(CIRCULATION_CHECK_IN_URL))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_COLLEGE)));

    // One local check in (Central tenant)
    wireMockServer.verify(1, postRequestedFor(urlPathMatching(CIRCULATION_CHECK_IN_URL))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_CONSORTIUM))
      .withRequestBody(equalToJson(asJsonString(checkInRequest))));
  }

  @Test
  @SneakyThrows
  void remoteCheckInWhenItemBarcodeFoundInSearchInstanceButCirculationItemDoesNotExist() {
    mockHelper.mockEcsTlrSettings(true);
    givenCurrentTenantIsConsortium();
    var checkInRequest = buildCheckInRequest();
    givenCirculationCheckInSucceeded(checkInRequest, randomId(), randomId());
    var itemId = randomId();
    givenSearchInstanceReturnsItem(TENANT_ID_COLLEGE,
      new SearchItem().id(itemId).barcode("test_barcode"));

    checkIn(checkInRequest).andExpect(status().isOk());

    // Zero local check ins
    wireMockServer.verify(0, postRequestedFor(urlPathMatching(CIRCULATION_CHECK_IN_URL))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_CONSORTIUM)));

    // One remote check in
    wireMockServer.verify(1, postRequestedFor(urlPathMatching(CIRCULATION_CHECK_IN_URL))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_COLLEGE))
      .withRequestBody(equalToJson(asJsonString(checkInRequest))));
  }

  @SneakyThrows
  private ResultActions checkIn(CheckInRequest checkInRequest) {
    return mockMvc.perform(post(CHECK_IN_URL)
      .content(asJsonString(checkInRequest))
      .headers(defaultHeaders()));
  }
}
