package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.folio.circulationbff.util.CirculationLoanTestData.HOLDINGS_RECORD_ID;
import static org.folio.circulationbff.util.CirculationLoanTestData.INSTANCE_ID;
import static org.folio.circulationbff.util.CirculationLoanTestData.IN_TRANSIT_DESTINATION_SERVICE_POINT_ID;
import static org.folio.circulationbff.util.CirculationLoanTestData.ITEM_ID;
import static org.folio.circulationbff.util.CirculationLoanTestData.LOAN_ID;
import static org.folio.circulationbff.util.CirculationLoanTestData.circulationLoan;
import static org.folio.circulationbff.util.CirculationLoanTestData.dcbLoanItem;
import static org.folio.circulationbff.util.CirculationLoanTestData.enrichedLoanItem;
import static org.folio.circulationbff.util.CirculationLoanTestData.inventoryInstance;
import static org.folio.circulationbff.util.CirculationLoanTestData.inventoryItem;
import static org.folio.circulationbff.util.CirculationLoanTestData.searchInstance;
import static org.folio.circulationbff.util.CirculationLoanTestData.searchItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.folio.circulationbff.domain.dto.CirculationLoan;
import org.folio.circulationbff.domain.dto.CirculationLoans;
import org.folio.circulationbff.domain.dto.Instance;
import org.folio.circulationbff.domain.dto.Instances;
import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.Items;
import org.folio.circulationbff.domain.dto.SearchInstance;
import org.folio.circulationbff.domain.dto.SearchInstances;
import org.folio.circulationbff.domain.dto.ServicePoint;
import org.folio.circulationbff.domain.dto.ServicePoints;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.folio.circulationbff.domain.dto.UserTenantCollection;
import org.folio.circulationbff.support.CqlQuery;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;

import com.github.tomakehurst.wiremock.client.WireMock;

class CirculationLoanApiTest extends BaseIT {

  @Test
  void findCirculationLoansForItem() throws Exception {
    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    mockCirculationLoansRequest(loanQuery, circulationLoan(USER_ID, false, enrichedLoanItem()));

    var expectedLoans = new CirculationLoans()
      .loans(List.of(circulationLoan(USER_ID, false, enrichedLoanItem())))
      .totalRecords(1);

    mockMvc.perform(get("/circulation-bff/loans")
        .queryParam("query", loanQuery)
        .param("limit", "2000")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedLoans)));
  }

  @Test
  void findCirculationLoansForItemWithUnknownFields() throws Exception {
    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    mockLoansRequestWithUnknownField(loanQuery);

    var expectedLoans = new CirculationLoans()
      .loans(List.of(circulationLoan(USER_ID, false, enrichedLoanItem())))
      .totalRecords(1);

    mockMvc.perform(get("/circulation-bff/loans")
        .queryParam("query", loanQuery)
        .param("limit", "2000")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedLoans)));
  }

  @ParameterizedTest
  @ValueSource(ints = { 400, 422, 500 })
  void findCirculationLoansForItemWhenCirculationReturnsError(int status) throws Exception {
    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    var responseBody = "Error response for status: " + status;
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/circulation/loans"))
      .withQueryParam("query", equalTo(loanQuery))
      .withQueryParam("limit", equalTo("2000"))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(aResponse().withStatus(status).withBody(responseBody)));

    mockMvc.perform(get("/circulation-bff/loans")
        .queryParam("query", loanQuery)
        .param("limit", "2000")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().is(status))
      .andExpect(content().string(responseBody));
  }

  @Test
  void findCirculationLoansForDcbItem() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);

    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    mockCirculationLoansRequest(loanQuery, circulationLoan(USER_ID, true, dcbLoanItem()));
    mockSearchRequest(List.of(ITEM_ID), searchInstance(searchItem()));
    mockItemStorageRequest(List.of(ITEM_ID), inventoryItem());
    mockServicePointsRequest();
    mockInstanceStorageRequest(List.of(INSTANCE_ID), inventoryInstance());

    var expectedLoans = new CirculationLoans()
      .loans(List.of(circulationLoan(USER_ID, true, enrichedLoanItem())))
      .totalRecords(1);

    mockMvc.perform(get("/circulation-bff/loans")
        .queryParam("query", loanQuery)
        .param("limit", "2000")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedLoans)));
  }

  @Test
  void findCirculationLoansForDcbItemWhenTlrDisabled() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(false);

    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    mockCirculationLoansRequest(loanQuery, circulationLoan(USER_ID, true, dcbLoanItem()));

    var expectedLoans = new CirculationLoans()
      .loans(List.of(circulationLoan(USER_ID, true, dcbLoanItem())))
      .totalRecords(1);

    mockMvc.perform(get("/circulation-bff/loans")
        .queryParam("query", loanQuery)
        .param("limit", "2000")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedLoans)));
  }

  @Test
  void getCirculationLoanByIdForItem() throws Exception {
    mockCirculationLoanBydRequest(circulationLoan(USER_ID, false, enrichedLoanItem()));
    var expectedLoan = circulationLoan(USER_ID, false, enrichedLoanItem());

    mockMvc.perform(get("/circulation-bff/loans/{loanId}", LOAN_ID)
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedLoan)));
  }

  @ParameterizedTest
  @ValueSource(ints = { 400, 422, 500 })
  void getCirculationLoanByIdForItemWhenCirculationReturnsError(int status) throws Exception {
    var responseBody = "Error response for status: " + status;
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/circulation/loans/" + LOAN_ID))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(aResponse().withStatus(status).withBody(responseBody)));

    mockMvc.perform(get("/circulation-bff/loans/{loanId}", LOAN_ID)
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().is(status))
      .andExpect(content().string(responseBody));
  }

  @Test
  void getCirculationLoanByIdForDcbItem() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);

    mockCirculationLoanBydRequest(circulationLoan(USER_ID, true, dcbLoanItem()));
    mockSearchRequest(List.of(ITEM_ID), searchInstance(searchItem()));
    mockItemStorageRequest(List.of(ITEM_ID), inventoryItem());
    mockServicePointsRequest();
    mockInstanceStorageRequest(List.of(INSTANCE_ID), inventoryInstance());

    var expectedLoan = circulationLoan(USER_ID, true, enrichedLoanItem());
    mockMvc.perform(get("/circulation-bff/loans/{loanId}", LOAN_ID)
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedLoan)));
  }

  @Test
  void getCirculationLoanByIdForDcbItemWhenTlrDisabled() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(false);

    mockCirculationLoanBydRequest(circulationLoan(USER_ID, true, dcbLoanItem()));

    var expectedLoan = circulationLoan(USER_ID, true, dcbLoanItem());
    mockMvc.perform(get("/circulation-bff/loans/{loanId}", LOAN_ID)
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedLoan)));
  }

  private static void mockCirculationLoansRequest(String loanQuery, CirculationLoan... loans) {
    var circulationLoans = new CirculationLoans()
      .loans(List.of(loans))
      .totalRecords(loans.length);

    wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/circulation/loans"))
      .withQueryParam("query", equalTo(loanQuery))
      .withQueryParam("limit", equalTo("2000"))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(asJsonString(circulationLoans), 200)));
  }

  private static void mockLoansRequestWithUnknownField(String loanQuery) {
    var circulationLoansJson = getCirculationLoanJsonWithUnknownFields();

    wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/circulation/loans"))
      .withQueryParam("query", equalTo(loanQuery))
      .withQueryParam("limit", equalTo("2000"))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(circulationLoansJson, 200)));
  }


  private static void mockCirculationLoanBydRequest(CirculationLoan loan) {
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/circulation/loans/" + LOAN_ID))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(asJsonString(loan), 200)));
  }

  private static void mockSearchRequest(List<String> itemIds, SearchInstance... instances) {
    var searchInstances = new SearchInstances()
      .instances(List.of(instances))
      .totalRecords(instances.length);

    wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/search/instances"))
      .withQueryParam("query", equalTo(CqlQuery.exactMatchAny("item.id", itemIds).toString()))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(asJsonString(searchInstances), 200)));
  }

  private static void mockItemStorageRequest(List<String> itemIds, Item... items) {
    var foundItems = new Items()
      .items(List.of(items))
      .totalRecords(items.length);

    wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/item-storage/items"))
      .withQueryParam("query", equalTo(CqlQuery.exactMatchAnyId(itemIds).toString()))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(asJsonString(foundItems), 200)));
  }

  private static void mockInstanceStorageRequest(List<String> instanceIds, Instance... instances) {
    var foundItems = new Instances()
      .instances(List.of(instances))
      .totalRecords(instances.length);

    wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/instance-storage/instances"))
      .withQueryParam("query", equalTo(CqlQuery.exactMatchAnyId(instanceIds).toString()))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(asJsonString(foundItems), 200)));
  }

  private static void mockServicePointsRequest() {
    var servicePointId = IN_TRANSIT_DESTINATION_SERVICE_POINT_ID.toString();
    var servicePoints = new ServicePoints()
      .servicepoints(List.of(new ServicePoint().id(servicePointId).name("test-library")))
      .totalRecords(1);

    var query = CqlQuery.exactMatchAny("id", List.of(servicePointId)).toString();
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo("/service-points"))
      .withQueryParam("query", equalTo(query))
      .withQueryParam("limit", equalTo("1"))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(asJsonString(servicePoints), 200)));
  }

  public UserTenantCollection buildUserTenant(String tenantId) {
    var userTenant = new UserTenant();
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    userTenant.setTenantId(tenantId);
    return new UserTenantCollection().addUserTenantsItem(userTenant);
  }


  private static String getCirculationLoanJsonWithUnknownFields() {
    return """
      {
        "loans": [
          {
            "id": "{loanId}",
            "userId": "{userId}",
            "itemId": "{itemId}",
            "unknownField": "unknownValue",
            "item": {
              "id": "{itemId}",
              "tenantId": "college",
              "title": "Test Item",
              "callNumber": "testCallNumber",
              "unknownField": "unknownValue",
              "callNumberComponents": {
                "callNumber": "testCallNumber",
                "prefix": "testCallNumber Prefix",
                "suffix": "testCallNumber Suffix",
                "unknownField": "unknownValue"
              },
              "copyNumber": "testCopyNumber",
              "editions": [
                "edition1",
                "edition2"
              ],
              "materialType": {
                "name": "text"
              },
              "contributors": [
                {
                  "name": "TestContributor1",
                  "unknownField": "unknownValue"
                },
                {
                  "name": "TestContributor2",
                  "unknownField": "unknownValue"
                },
                {
                  "name": "TestContributor3",
                  "unknownField": "unknownValue"
                }
              ],
              "primaryContributor": "TestContributor2",
              "holdingsRecordId": "{holdingsRecordId}",
              "instanceId": "{instanceId}",
              "instanceHrid": "in00000000001",
              "barcode": "testbarcode",
              "location": {
                "name": "DCB"
              },
              "status": {
                "name": "Checked Out",
                "date": "1970-01-01T00:00:01.000+00:00",
                "unknownField": "unknownValue"
              },
              "inTransitDestinationServicePointId": "{inTransitDSPId}",
              "inTransitDestinationServicePoint": {
                "id": "{inTransitDSPId}",
                "name": "test-library",
                "unknownField": "unknownValue"
              },
              "enumeration": "testEnumeration",
              "chronology": "testChronology",
              "volume": "testVolume",
              "displaySummary": "testDisplaySummary",
              "datesOfPublication": [
                "1999",
                "2000"
              ],
              "accessionNumber": "testAccessionNumber",
              "physicalDescriptions": [
                "testPhysicalDescription1",
                "testPhysicalDescription2"
              ]
            },
            "isDcb": false
          }
        ],
        "totalRecords": 1
      }"""
      .replaceAll("\\{loanId}", LOAN_ID)
      .replaceAll("\\{userId}", USER_ID)
      .replaceAll("\\{itemId}", ITEM_ID)
      .replaceAll("\\{holdingsRecordId}", HOLDINGS_RECORD_ID)
      .replaceAll("\\{instanceId}", INSTANCE_ID)
      .replaceAll("\\{inTransitDSPId}", IN_TRANSIT_DESTINATION_SERVICE_POINT_ID.toString());
  }
}
