package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import org.folio.circulationbff.domain.dto.CirculationLoan;
import org.folio.circulationbff.domain.dto.CirculationLoans;
import org.folio.circulationbff.domain.dto.ConsortiumItem;
import org.folio.circulationbff.domain.dto.ConsortiumItemBatchRequest;
import org.folio.circulationbff.domain.dto.ConsortiumItemCollection;
import org.folio.circulationbff.domain.dto.HoldingsRecord;
import org.folio.circulationbff.domain.dto.HoldingsRecords;
import org.folio.circulationbff.domain.dto.Instance;
import org.folio.circulationbff.domain.dto.Instances;
import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.Items;
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
import org.springframework.test.context.TestPropertySource;

import com.github.tomakehurst.wiremock.client.WireMock;

@TestPropertySource(properties = { "folio.tenant.secure-tenant-id=secure" })
class CirculationLoanApiTest extends BaseIT {

  private static final String LOANS_URL = "/circulation-bff/loans";
  private static final String LOAN_BY_ID_URL = "/circulation-bff/loans/{loanId}";
  private static final String CIRCULATION_LOANS_URL = "/circulation/loans";
  private static final String SEARCH_INSTANCES_URL = "/search/instances";
  private static final String ITEM_STORAGE_URL = "/item-storage/items";
  private static final String INSTANCE_STORAGE_URL = "/instance-storage/instances";
  private static final String SERVICE_POINTS_URL = "/service-points";
  private static final String BATCH_ITEMS_URL = "/search/consortium/batch/items";
  private static final String HOLDINGS_STORAGE_URL = "/holdings-storage/holdings";
  private static final String LOCATIONS_URL = "/locations";
  private static final String MATERIAL_TYPES_URL = "/material-types";
  private static final String LOAN_TYPES_URL = "/loan-types";
  private static final String TENANT_ID_UNIVERSITY = "university";

  @Test
  void findCirculationLoansForItem() throws Exception {
    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    mockCirculationLoansRequest(loanQuery, circulationLoan(USER_ID, false, enrichedLoanItem()));

    var expectedLoans = new CirculationLoans()
      .loans(List.of(circulationLoan(USER_ID, false, enrichedLoanItem())))
      .totalRecords(1);

    mockMvc.perform(get(LOANS_URL)
        .queryParam("query", loanQuery)
        .param("limit", "500")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedLoans)));
  }

  @Test
  void findCirculationLoansForwardsLargeLimit() throws Exception {
    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    mockCirculationLoansRequest(loanQuery, "100000", TENANT_ID_CONSORTIUM,
      circulationLoan(USER_ID, false, enrichedLoanItem()));

    mockMvc.perform(get(LOANS_URL)
        .queryParam("query", loanQuery)
        .param("limit", "100000")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.loans.length()").value(1));

    wireMockServer.verify(1, getRequestedFor(urlPathEqualTo(CIRCULATION_LOANS_URL))
      .withQueryParam("limit", equalTo("100000")));
  }

  @Test
  void findCirculationLoansForItemWithUnknownFields() throws Exception {
    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    mockLoansRequestWithUnknownField(loanQuery);

    var expectedLoans = new CirculationLoans()
      .loans(List.of(circulationLoan(USER_ID, false, enrichedLoanItem())))
      .totalRecords(1);

    mockMvc.perform(get(LOANS_URL)
        .queryParam("query", loanQuery)
        .param("limit", "500")
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
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(CIRCULATION_LOANS_URL))
      .withQueryParam("query", equalTo(loanQuery))
      .withQueryParam("limit", equalTo("500"))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(aResponse().withStatus(status).withBody(responseBody)));

    mockMvc.perform(get(LOANS_URL)
        .queryParam("query", loanQuery)
        .param("limit", "500")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().is(status))
      .andExpect(content().string(responseBody));
  }

  @Test
  void findCirculationLoansForDcbItemForNonCentralTenant() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_COLLEGE);
    mockHelper.mockEcsTlrCirculationSettings(true, TENANT_ID_COLLEGE);

    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    var circulationLoan = circulationLoan(USER_ID, true, dcbLoanItem());

    mockCirculationLoansRequest(loanQuery, TENANT_ID_COLLEGE, circulationLoan);

    var expectedLoans = new CirculationLoans()
      .loans(List.of(circulationLoan))
      .totalRecords(1);

    mockMvc.perform(get(LOANS_URL)
        .queryParam("query", loanQuery)
        .param("limit", "500")
        .headers(buildHeaders(TENANT_ID_COLLEGE))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedLoans)));
  }

  @Test
  void findCirculationLoansForDcbItemForNonCentralSecureTenant() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_SECURE);
    mockHelper.mockEcsTlrCirculationSettings(true, TENANT_ID_SECURE);

    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    var circulationLoan = circulationLoan(USER_ID, true, dcbLoanItem());

    mockCirculationLoansRequest(loanQuery, TENANT_ID_SECURE, circulationLoan);
    mockBatchItemsRequest(List.of(ITEM_ID), consortiumItem(ITEM_ID, TENANT_ID_COLLEGE));
    mockItemStorageRequest(List.of(ITEM_ID), inventoryItem());
    mockServicePointsRequest();
    mockInstanceStorageRequest(List.of(INSTANCE_ID), TENANT_ID_COLLEGE, inventoryInstance());

    var expectedLoans = new CirculationLoans()
      .loans(List.of(circulationLoan(USER_ID, true, enrichedLoanItem())))
      .totalRecords(1);

    mockMvc.perform(get(LOANS_URL)
        .queryParam("query", loanQuery)
        .param("limit", "500")
        .headers(buildHeaders(TENANT_ID_SECURE))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedLoans)));

    // a secure tenant must search in the central tenant scope
    wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(BATCH_ITEMS_URL))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID_CONSORTIUM)));
  }

  @Test
  void findCirculationLoansForDcbItem() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);

    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    mockCirculationLoansRequest(loanQuery, circulationLoan(USER_ID, true, dcbLoanItem()));
    mockBatchItemsRequest(List.of(ITEM_ID), consortiumItem(ITEM_ID, TENANT_ID_COLLEGE));
    mockItemStorageRequest(List.of(ITEM_ID), inventoryItem());
    mockServicePointsRequest();
    mockInstanceStorageRequest(List.of(INSTANCE_ID), TENANT_ID_COLLEGE, inventoryInstance());

    var expectedLoans = new CirculationLoans()
      .loans(List.of(circulationLoan(USER_ID, true, enrichedLoanItem())))
      .totalRecords(1);

    mockMvc.perform(get(LOANS_URL)
        .queryParam("query", loanQuery)
        .param("limit", "500")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedLoans)));
  }

  @Test
  void findCirculationLoansDoesNotCallSearchInstances() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);
    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    mockCirculationLoansRequest(loanQuery, circulationLoan(USER_ID, true, dcbLoanItem()));
    mockDcbItemFlow(TENANT_ID_COLLEGE);

    mockMvc.perform(get(LOANS_URL)
        .queryParam("query", loanQuery)
        .param("limit", "500")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.loans[0].item.tenantId").value(TENANT_ID_COLLEGE));

    wireMockServer.verify(0, getRequestedFor(urlPathEqualTo(SEARCH_INSTANCES_URL)));
  }

  @Test
  void findCirculationLoansOnlyRequestsLoanedItemsFromStorage() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);
    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    mockCirculationLoansRequest(loanQuery, circulationLoan(USER_ID, true, dcbLoanItem()));
    mockDcbItemFlow(TENANT_ID_COLLEGE);

    mockMvc.perform(get(LOANS_URL)
        .queryParam("query", loanQuery)
        .param("limit", "500")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk());

    wireMockServer.verify(1, getRequestedFor(urlPathEqualTo(ITEM_STORAGE_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathEqualTo(HOLDINGS_STORAGE_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathEqualTo(LOCATIONS_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathEqualTo(MATERIAL_TYPES_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathEqualTo(LOAN_TYPES_URL)));
  }

  @Test
  void findCirculationLoansSplitsBatchCallsIntoChunks() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);
    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    var loans = IntStream.range(0, 250)
      .mapToObj(i -> dcbLoan("chunk-loan-" + i, UUID.randomUUID().toString()))
      .toArray(CirculationLoan[]::new);
    mockCirculationLoansRequest(loanQuery, "250", TENANT_ID_CONSORTIUM, loans);
    mockAnyBatchItemsRequest();

    mockMvc.perform(get(LOANS_URL)
        .queryParam("query", loanQuery)
        .param("limit", "250")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.loans.length()").value(250))
      .andExpect(jsonPath("$.loans[0].item.tenantId").doesNotExist());

    wireMockServer.verify(3, postRequestedFor(urlPathEqualTo(BATCH_ITEMS_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathEqualTo(ITEM_STORAGE_URL)));
  }

  @Test
  void findCirculationLoansWithItemsFromTwoTenants() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);
    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    var collegeItemId = UUID.randomUUID().toString();
    var universityItemId = UUID.randomUUID().toString();
    var collegeInstanceId = UUID.randomUUID().toString();
    var universityInstanceId = UUID.randomUUID().toString();
    mockCirculationLoansRequest(loanQuery,
      dcbLoan("college-loan", collegeItemId, collegeInstanceId),
      dcbLoan("university-loan", universityItemId, universityInstanceId));
    mockBatchItemsRequest(List.of(collegeItemId, universityItemId),
      consortiumItem(collegeItemId, TENANT_ID_COLLEGE, collegeInstanceId),
      consortiumItem(universityItemId, TENANT_ID_UNIVERSITY, universityInstanceId));
    mockItemStorageRequest(List.of(collegeItemId), TENANT_ID_COLLEGE,
      inventoryItem().id(collegeItemId).inTransitDestinationServicePointId(null));
    mockItemStorageRequest(List.of(universityItemId), TENANT_ID_UNIVERSITY,
      inventoryItem().id(universityItemId).inTransitDestinationServicePointId(null));
    mockInstanceStorageRequest(List.of(collegeInstanceId), TENANT_ID_COLLEGE,
      inventoryInstance().id(collegeInstanceId));
    mockInstanceStorageRequest(List.of(universityInstanceId), TENANT_ID_UNIVERSITY,
      inventoryInstance().id(universityInstanceId));

    mockMvc.perform(get(LOANS_URL)
        .queryParam("query", loanQuery)
        .param("limit", "500")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.loans[0].item.tenantId").value(TENANT_ID_COLLEGE))
      .andExpect(jsonPath("$.loans[0].item.callNumber").value("testCallNumber"))
      .andExpect(jsonPath("$.loans[1].item.tenantId").value(TENANT_ID_UNIVERSITY))
      .andExpect(jsonPath("$.loans[1].item.callNumber").value("testCallNumber"));

    wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(BATCH_ITEMS_URL)));
  }

  @Test
  void findCirculationLoansWhenItemMissingFromSearch() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);
    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    mockCirculationLoansRequest(loanQuery, circulationLoan(USER_ID, true, dcbLoanItem()));
    mockBatchItemsRequest(List.of(ITEM_ID));

    var expectedLoans = new CirculationLoans()
      .loans(List.of(circulationLoan(USER_ID, true, dcbLoanItem())))
      .totalRecords(1);

    mockMvc.perform(get(LOANS_URL)
        .queryParam("query", loanQuery)
        .param("limit", "500")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedLoans)));

    wireMockServer.verify(0, getRequestedFor(urlPathEqualTo(ITEM_STORAGE_URL)));
  }

  @Test
  void findCirculationLoansFallsBackToCentralInstance() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);
    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    mockCirculationLoansRequest(loanQuery, circulationLoan(USER_ID, true, dcbLoanItem()));
    mockBatchItemsRequest(List.of(ITEM_ID), consortiumItem(ITEM_ID, TENANT_ID_COLLEGE));
    mockItemStorageRequest(List.of(ITEM_ID), inventoryItem());
    mockServicePointsRequest();
    mockInstanceStorageRequest(List.of(INSTANCE_ID), TENANT_ID_COLLEGE);
    mockInstanceStorageRequest(List.of(INSTANCE_ID), TENANT_ID_CONSORTIUM, inventoryInstance());

    var expectedLoans = new CirculationLoans()
      .loans(List.of(circulationLoan(USER_ID, true, enrichedLoanItem())))
      .totalRecords(1);

    mockMvc.perform(get(LOANS_URL)
        .queryParam("query", loanQuery)
        .param("limit", "500")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedLoans)));

    wireMockServer.verify(1, getRequestedFor(urlPathEqualTo(INSTANCE_STORAGE_URL))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID_COLLEGE)));
    wireMockServer.verify(1, getRequestedFor(urlPathEqualTo(INSTANCE_STORAGE_URL))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID_CONSORTIUM)));
  }

  @Test
  void findCirculationLoansUsesHoldingsCopyNumberWhenItemHasNone() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);
    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    mockCirculationLoansRequest(loanQuery, circulationLoan(USER_ID, true, dcbLoanItem()));
    mockBatchItemsRequest(List.of(ITEM_ID), consortiumItem(ITEM_ID, TENANT_ID_COLLEGE));
    mockItemStorageRequest(List.of(ITEM_ID),
      inventoryItem().copyNumber(null).holdingsRecordId(HOLDINGS_RECORD_ID));
    mockHoldingsStorageRequest(List.of(HOLDINGS_RECORD_ID), TENANT_ID_COLLEGE,
      new HoldingsRecord().id(HOLDINGS_RECORD_ID).copyNumber("holdingsCopy"));
    mockServicePointsRequest();
    mockInstanceStorageRequest(List.of(INSTANCE_ID), TENANT_ID_COLLEGE, inventoryInstance());

    mockMvc.perform(get(LOANS_URL)
        .queryParam("query", loanQuery)
        .param("limit", "500")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.loans[0].item.copyNumber").value("holdingsCopy"));
  }

  @Test
  void findCirculationLoansWhenHoldingsMissing() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);
    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    mockCirculationLoansRequest(loanQuery, circulationLoan(USER_ID, true, dcbLoanItem()));
    mockBatchItemsRequest(List.of(ITEM_ID), consortiumItem(ITEM_ID, TENANT_ID_COLLEGE));
    mockItemStorageRequest(List.of(ITEM_ID),
      inventoryItem().copyNumber(null).holdingsRecordId(HOLDINGS_RECORD_ID));
    mockHoldingsStorageRequest(List.of(HOLDINGS_RECORD_ID), TENANT_ID_COLLEGE);
    mockServicePointsRequest();
    mockInstanceStorageRequest(List.of(INSTANCE_ID), TENANT_ID_COLLEGE, inventoryInstance());

    mockMvc.perform(get(LOANS_URL)
        .queryParam("query", loanQuery)
        .param("limit", "500")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.loans[0].item.tenantId").value(TENANT_ID_COLLEGE))
      .andExpect(jsonPath("$.loans[0].item.copyNumber").doesNotExist());
  }

  @Test
  void findCirculationLoansReturnsUnenrichedLoansWhenSearchFails() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);
    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    mockCirculationLoansRequest(loanQuery, circulationLoan(USER_ID, true, dcbLoanItem()));
    wireMockServer.stubFor(WireMock.post(urlPathEqualTo(BATCH_ITEMS_URL))
      .willReturn(aResponse().withStatus(500).withBody("search is down")));

    var expectedLoans = new CirculationLoans()
      .loans(List.of(circulationLoan(USER_ID, true, dcbLoanItem())))
      .totalRecords(1);

    mockMvc.perform(get(LOANS_URL)
        .queryParam("query", loanQuery)
        .param("limit", "500")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedLoans)));

    wireMockServer.verify(0, getRequestedFor(urlPathEqualTo(ITEM_STORAGE_URL)));
  }

  @Test
  void findCirculationLoansProductionShapedCardinality() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);
    var loanQuery = String.format("(userId==%s) sortby id", USER_ID);
    var loans = new ArrayList<CirculationLoan>();
    var itemIds = new ArrayList<String>();
    var instanceIds = new ArrayList<String>();
    var consortiumItems = new ArrayList<ConsortiumItem>();
    var storageItems = new ArrayList<Item>();
    var instances = new ArrayList<Instance>();
    for (int i = 0; i < 49; i++) {
      var itemId = UUID.randomUUID().toString();
      var instanceId = UUID.randomUUID().toString();
      itemIds.add(itemId);
      instanceIds.add(instanceId);
      loans.add(dcbLoan("loan-" + i, itemId, instanceId));
      consortiumItems.add(consortiumItem(itemId, TENANT_ID_COLLEGE, instanceId));
      storageItems.add(inventoryItem().id(itemId).inTransitDestinationServicePointId(null));
      instances.add(inventoryInstance().id(instanceId));
    }
    mockCirculationLoansRequest(loanQuery, "49", TENANT_ID_CONSORTIUM,
      loans.toArray(CirculationLoan[]::new));
    mockBatchItemsRequest(itemIds, consortiumItems.toArray(ConsortiumItem[]::new));
    mockItemStorageRequest(itemIds, storageItems.toArray(Item[]::new));
    mockInstanceStorageRequest(instanceIds, TENANT_ID_COLLEGE,
      instances.toArray(Instance[]::new));

    mockMvc.perform(get(LOANS_URL)
        .queryParam("query", loanQuery)
        .param("limit", "49")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.loans.length()").value(49))
      .andExpect(jsonPath("$.loans[0].item.tenantId").value(TENANT_ID_COLLEGE))
      .andExpect(jsonPath("$.loans[48].item.tenantId").value(TENANT_ID_COLLEGE))
      .andExpect(jsonPath("$.loans[48].item.callNumber").value("testCallNumber"));

    wireMockServer.verify(1, postRequestedFor(urlPathEqualTo(BATCH_ITEMS_URL)));
    wireMockServer.verify(1, getRequestedFor(urlPathEqualTo(ITEM_STORAGE_URL)));
    wireMockServer.verify(1, getRequestedFor(urlPathEqualTo(INSTANCE_STORAGE_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathEqualTo(HOLDINGS_STORAGE_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathEqualTo(SEARCH_INSTANCES_URL)));
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

    mockMvc.perform(get(LOANS_URL)
        .queryParam("query", loanQuery)
        .param("limit", "500")
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedLoans)));
  }

  @Test
  void getCirculationLoanByIdForItem() throws Exception {
    mockCirculationLoanBydRequest(circulationLoan(USER_ID, false, enrichedLoanItem()));
    var expectedLoan = circulationLoan(USER_ID, false, enrichedLoanItem());

    mockMvc.perform(get(LOAN_BY_ID_URL, LOAN_ID)
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedLoan)));
  }

  @ParameterizedTest
  @ValueSource(ints = { 400, 422, 500 })
  void getCirculationLoanByIdForItemWhenCirculationReturnsError(int status) throws Exception {
    var responseBody = "Error response for status: " + status;
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(CIRCULATION_LOANS_URL + "/" + LOAN_ID))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(aResponse().withStatus(status).withBody(responseBody)));

    mockMvc.perform(get(LOAN_BY_ID_URL, LOAN_ID)
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
    mockBatchItemsRequest(List.of(ITEM_ID), consortiumItem(ITEM_ID, TENANT_ID_COLLEGE));
    mockItemStorageRequest(List.of(ITEM_ID), inventoryItem());
    mockServicePointsRequest();
    mockInstanceStorageRequest(List.of(INSTANCE_ID), TENANT_ID_COLLEGE, inventoryInstance());

    var expectedLoan = circulationLoan(USER_ID, true, enrichedLoanItem());
    mockMvc.perform(get(LOAN_BY_ID_URL, LOAN_ID)
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedLoan)));
  }


  @Test
  void getCirculationLoanByIdForDcbItemDoesNotCallSearchInstances() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);
    mockCirculationLoanBydRequest(circulationLoan(USER_ID, true, dcbLoanItem()));
    mockDcbItemFlow(TENANT_ID_COLLEGE);

    mockMvc.perform(get(LOAN_BY_ID_URL, LOAN_ID)
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.item.tenantId").value(TENANT_ID_COLLEGE));

    wireMockServer.verify(0, getRequestedFor(urlPathEqualTo(SEARCH_INSTANCES_URL)));
  }

  @Test
  void getCirculationLoanByIdForDcbItemForNonCentralTenant() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_COLLEGE);
    mockHelper.mockEcsTlrCirculationSettings(true, TENANT_ID_COLLEGE);

    var circulationLoan = circulationLoan(USER_ID, true, dcbLoanItem());
    mockCirculationLoanBydRequest(TENANT_ID_COLLEGE, circulationLoan);

    mockMvc.perform(get(LOAN_BY_ID_URL, LOAN_ID)
        .headers(buildHeaders(TENANT_ID_COLLEGE))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(circulationLoan)));
  }

  @Test
  void getCirculationLoanByIdForDcbItemForNonCentralSecureTenant() throws Exception {
    mockHelper.mockUserTenants(buildUserTenant(TENANT_ID_CONSORTIUM), TENANT_ID_SECURE);
    mockHelper.mockEcsTlrCirculationSettings(true, TENANT_ID_SECURE);

    mockCirculationLoanBydRequest(TENANT_ID_SECURE, circulationLoan(USER_ID, true, dcbLoanItem()));
    mockBatchItemsRequest(List.of(ITEM_ID), consortiumItem(ITEM_ID, TENANT_ID_COLLEGE));
    mockItemStorageRequest(List.of(ITEM_ID), inventoryItem());
    mockServicePointsRequest();
    mockInstanceStorageRequest(List.of(INSTANCE_ID), TENANT_ID_COLLEGE, inventoryInstance());

    var expectedLoan = circulationLoan(USER_ID, true, enrichedLoanItem());
    mockMvc.perform(get(LOAN_BY_ID_URL, LOAN_ID)
        .headers(buildHeaders(TENANT_ID_SECURE))
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
    mockMvc.perform(get(LOAN_BY_ID_URL, LOAN_ID)
        .headers(buildHeaders(TENANT_ID_CONSORTIUM))
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(asJsonString(expectedLoan)));
  }

  private static void mockCirculationLoansRequest(String loanQuery, CirculationLoan... loans) {
    mockCirculationLoansRequest(loanQuery, TENANT_ID_CONSORTIUM, loans);
  }

  private static void mockCirculationLoansRequest(String loanQuery, String tenantId,
    CirculationLoan... loans) {

    mockCirculationLoansRequest(loanQuery, "500", tenantId, loans);
  }

  private static void mockCirculationLoansRequest(String loanQuery, String limit,
    String tenantId, CirculationLoan... loans) {

    var circulationLoans = new CirculationLoans()
      .loans(List.of(loans))
      .totalRecords(loans.length);

    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(CIRCULATION_LOANS_URL))
      .withQueryParam("query", equalTo(loanQuery))
      .withQueryParam("limit", equalTo(limit))
      .withHeader(XOkapiHeaders.TENANT, equalTo(tenantId))
      .willReturn(jsonResponse(asJsonString(circulationLoans), 200)));
  }

  private static void mockLoansRequestWithUnknownField(String loanQuery) {
    var circulationLoansJson = getCirculationLoanJsonWithUnknownFields();

    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(CIRCULATION_LOANS_URL))
      .withQueryParam("query", equalTo(loanQuery))
      .withQueryParam("limit", equalTo("500"))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(circulationLoansJson, 200)));
  }

  private static void mockCirculationLoanBydRequest(CirculationLoan loan) {
    mockCirculationLoanBydRequest(TENANT_ID_CONSORTIUM, loan);
  }

  private static void mockCirculationLoanBydRequest(String tenantId, CirculationLoan loan) {
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(CIRCULATION_LOANS_URL + "/" + LOAN_ID))
      .withHeader(XOkapiHeaders.TENANT, equalTo(tenantId))
      .willReturn(jsonResponse(asJsonString(loan), 200)));
  }

  private static CirculationLoan dcbLoan(String loanId, String itemId) {
    return dcbLoan(loanId, itemId, UUID.randomUUID().toString());
  }

  private static CirculationLoan dcbLoan(String loanId, String itemId, String instanceId) {
    var item = dcbLoanItem().id(itemId).instanceId(instanceId);
    return circulationLoan(USER_ID, true, item).id(loanId).itemId(itemId);
  }

  /** Stubs the whole happy path for the default DCB item living in the given tenant. */
  private static void mockDcbItemFlow(String itemTenantId) {
    mockBatchItemsRequest(List.of(ITEM_ID), consortiumItem(ITEM_ID, itemTenantId));
    mockItemStorageRequest(List.of(ITEM_ID), itemTenantId, inventoryItem());
    mockServicePointsRequest(itemTenantId);
    mockInstanceStorageRequest(List.of(INSTANCE_ID), itemTenantId, inventoryInstance());
  }

  /** The batch search is always executed in the central tenant scope. */
  private static void mockBatchItemsRequest(List<String> itemIds, ConsortiumItem... items) {
    var response = new ConsortiumItemCollection()
      .items(List.of(items))
      .totalRecords(items.length);

    wireMockServer.stubFor(WireMock.post(urlPathEqualTo(BATCH_ITEMS_URL))
      .withRequestBody(matchingJsonPath("$.identifierType", equalTo("id")))
      .withRequestBody(equalToJson(asJsonString(new ConsortiumItemBatchRequest()
        .identifierType("id")
        .identifierValues(itemIds)), true, true))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(asJsonString(response), 200)));
  }

  /** Matches any batch request, regardless of the requested ids. */
  private static void mockAnyBatchItemsRequest(ConsortiumItem... items) {
    var response = new ConsortiumItemCollection()
      .items(List.of(items))
      .totalRecords(items.length);

    wireMockServer.stubFor(WireMock.post(urlPathEqualTo(BATCH_ITEMS_URL))
      .withRequestBody(matchingJsonPath("$.identifierType", equalTo("id")))
      .withHeader(XOkapiHeaders.TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(asJsonString(response), 200)));
  }

  private static ConsortiumItem consortiumItem(String itemId, String tenantId) {
    return consortiumItem(itemId, tenantId, INSTANCE_ID);
  }

  private static ConsortiumItem consortiumItem(String itemId, String tenantId,
    String instanceId) {

    return new ConsortiumItem()
      .id(itemId)
      .tenantId(tenantId)
      .instanceId(instanceId)
      .holdingsRecordId(HOLDINGS_RECORD_ID);
  }

  private static void mockItemStorageRequest(List<String> itemIds, Item... items) {
    mockItemStorageRequest(itemIds, TENANT_ID_COLLEGE, items);
  }

  private static void mockItemStorageRequest(List<String> itemIds, String tenantId,
    Item... items) {

    var foundItems = new Items()
      .items(List.of(items))
      .totalRecords(items.length);

    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(ITEM_STORAGE_URL))
      .withQueryParam("query", equalTo(CqlQuery.exactMatchAnyId(itemIds).toString()))
      .withHeader(XOkapiHeaders.TENANT, equalTo(tenantId))
      .willReturn(jsonResponse(asJsonString(foundItems), 200)));
  }

  private static void mockInstanceStorageRequest(List<String> instanceIds, String tenantId,
    Instance... instances) {

    var foundInstances = new Instances()
      .instances(List.of(instances))
      .totalRecords(instances.length);

    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(INSTANCE_STORAGE_URL))
      .withQueryParam("query", equalTo(CqlQuery.exactMatchAnyId(instanceIds).toString()))
      .withHeader(XOkapiHeaders.TENANT, equalTo(tenantId))
      .willReturn(jsonResponse(asJsonString(foundInstances), 200)));
  }

  private static void mockHoldingsStorageRequest(List<String> holdingsIds, String tenantId,
    HoldingsRecord... holdings) {

    var foundHoldings = new HoldingsRecords()
      .holdingsRecords(List.of(holdings))
      .totalRecords(holdings.length);

    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(HOLDINGS_STORAGE_URL))
      .withQueryParam("query", equalTo(CqlQuery.exactMatchAnyId(holdingsIds).toString()))
      .withHeader(XOkapiHeaders.TENANT, equalTo(tenantId))
      .willReturn(jsonResponse(asJsonString(foundHoldings), 200)));
  }

  private static void mockServicePointsRequest() {
    mockServicePointsRequest(TENANT_ID_COLLEGE);
  }

  private static void mockServicePointsRequest(String tenantId) {
    var servicePointId = IN_TRANSIT_DESTINATION_SERVICE_POINT_ID.toString();
    var servicePoints = new ServicePoints()
      .servicepoints(List.of(new ServicePoint().id(servicePointId).name("test-library")))
      .totalRecords(1);

    var query = CqlQuery.exactMatchAny("id", List.of(servicePointId)).toString();
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(SERVICE_POINTS_URL))
      .withQueryParam("query", equalTo(query))
      .withQueryParam("limit", equalTo("1"))
      .withHeader(XOkapiHeaders.TENANT, equalTo(tenantId))
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
