package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.Collections.emptyList;
import static org.folio.circulationbff.service.BulkFetchingService.MAX_IDS_PER_QUERY;
import static org.folio.circulationbff.util.ApiEndpointURL.HOLDINGS_STORAGE_URL;
import static org.folio.circulationbff.util.ApiEndpointURL.INSTANCE_STORAGE_URL;
import static org.folio.circulationbff.util.ApiEndpointURL.ITEM_STORAGE_URL;
import static org.folio.circulationbff.util.ApiEndpointURL.LOAN_TYPES_URL;
import static org.folio.circulationbff.util.ApiEndpointURL.LOCATIONS_URL;
import static org.folio.circulationbff.util.ApiEndpointURL.MATERIAL_TYPES_URL;
import static org.folio.circulationbff.util.ApiEndpointURL.SEARCH_INSTANCES_MOD_SEARCH_URL;
import static org.folio.circulationbff.util.ApiEndpointURL.SEARCH_INSTANCES_URL;
import static org.folio.circulationbff.util.ApiEndpointURL.SERVICE_POINTS_URL;
import static org.folio.circulationbff.util.MockHelper.buildHoldingsRecords;
import static org.folio.circulationbff.util.MockHelper.buildItems;
import static org.folio.circulationbff.util.MockHelper.buildLocations;
import static org.folio.circulationbff.util.MockHelper.buildMaterialTypes;
import static org.folio.circulationbff.util.MockHelper.buildSearchHolding;
import static org.folio.circulationbff.util.MockHelper.buildSearchInstance;
import static org.folio.circulationbff.util.MockHelper.buildSearchItems;
import static org.folio.circulationbff.util.MockHelper.buildServicePoints;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.http.HttpStatus;
import org.folio.circulationbff.domain.dto.HoldingsRecords;
import org.folio.circulationbff.domain.dto.Instance;
import org.folio.circulationbff.domain.dto.Instances;
import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.Items;
import org.folio.circulationbff.domain.dto.LoanType;
import org.folio.circulationbff.domain.dto.LoanTypes;
import org.folio.circulationbff.domain.dto.Locations;
import org.folio.circulationbff.domain.dto.MaterialTypes;
import org.folio.circulationbff.domain.dto.SearchHolding;
import org.folio.circulationbff.domain.dto.SearchInstance;
import org.folio.circulationbff.domain.dto.SearchInstances;
import org.folio.circulationbff.domain.dto.SearchItem;
import org.folio.circulationbff.domain.dto.ServicePoints;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.MediaType;

import com.github.tomakehurst.wiremock.client.WireMock;

import lombok.SneakyThrows;

class SearchInstancesApiTest extends BaseIT {

  private static final String PERMANENT_LOAN_TYPE_ID = "22fa71d319-997b-4a60-8cfd-20fdf57efa14";
  private static final String TEMPORARY_LOAN_TYPE_ID = "2286d4aed0-c76b-4907-983f-1327dfb4b12d";
  @Mock private SystemUserScopedExecutionService systemUserScopedExecutionService;

  @Test
  @SneakyThrows
  void searchFindsNoInstances() {
    String instanceId = randomId();

    wireMockServer.stubFor(WireMock.get(urlPathMatching(SEARCH_INSTANCES_MOD_SEARCH_URL))
      .withQueryParam("query", equalTo("id==" + instanceId))
      .withQueryParam("expandAll", equalTo("true"))
      .willReturn(jsonResponse(new SearchInstances().instances(emptyList()), HttpStatus.SC_OK)));

    mockMvc.perform(
        get(SEARCH_INSTANCES_URL)
          .queryParam("query", "id==" + instanceId)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json("{}"));

    wireMockServer.verify(0, getRequestedFor(urlPathMatching(ITEM_STORAGE_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathMatching(HOLDINGS_STORAGE_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathMatching(LOCATIONS_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathMatching(SERVICE_POINTS_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathMatching(MATERIAL_TYPES_URL)));
  }

  @Test
  @SneakyThrows
  void searchFindsNoItems() {
    String instanceId = randomId();

    SearchInstances mockSearchInstancesResponse = new SearchInstances()
      .instances(List.of(
        new SearchInstance()
          .id(instanceId)
          .items(emptyList())
          .tenantId(TENANT_ID_CONSORTIUM)
      ));

    wireMockServer.stubFor(WireMock.get(urlPathMatching(SEARCH_INSTANCES_MOD_SEARCH_URL))
      .withQueryParam("query", equalTo("id==" + instanceId))
      .withQueryParam("expandAll", equalTo("true"))
      .willReturn(jsonResponse(asJsonString(mockSearchInstancesResponse), HttpStatus.SC_OK)));

    Instance instance = new Instance().id(instanceId).editions(Set.of("1st", "2st"));
    Instances instances = new Instances().instances(List.of(instance));
    createStubForGetByIds(INSTANCE_STORAGE_URL, TENANT_ID_CONSORTIUM, instances);

    when(systemUserScopedExecutionService.executeSystemUserScoped(any(String.class), any()))
      .thenReturn(emptyList());

    mockMvc.perform(
        get(SEARCH_INSTANCES_URL)
          .queryParam("query", "id==" + instanceId)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id", is(instanceId)))
      .andExpect(jsonPath("$.items", emptyIterable()))
      .andExpect(jsonPath("$.editions", containsInAnyOrder("1st", "2st")));

    wireMockServer.verify(0, getRequestedFor(urlPathMatching(ITEM_STORAGE_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathMatching(HOLDINGS_STORAGE_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathMatching(LOCATIONS_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathMatching(SERVICE_POINTS_URL)));
    wireMockServer.verify(0, getRequestedFor(urlPathMatching(MATERIAL_TYPES_URL)));
  }

  @Test
  @SneakyThrows
  void searchInstancesReturnsOkStatus() {
    // mock instance search

    SearchHolding searchHoldingInConsortium = buildSearchHolding(TENANT_ID_CONSORTIUM);
    SearchHolding searchHoldingInCollege = buildSearchHolding(TENANT_ID_COLLEGE);

    List<SearchItem> searchItemsInConsortium = buildSearchItems(MAX_IDS_PER_QUERY, TENANT_ID_CONSORTIUM,
      searchHoldingInConsortium.getId());
    List<SearchItem> searchItemsInCollege = buildSearchItems(10, TENANT_ID_COLLEGE,
      searchHoldingInCollege.getId());
    List<SearchItem> allSearchItems = Stream.concat(searchItemsInConsortium.stream(),
        searchItemsInCollege.stream())
      .toList();

    SearchInstance searchInstance = buildSearchInstance(TENANT_ID_CONSORTIUM, allSearchItems,
      List.of(searchHoldingInConsortium, searchHoldingInCollege));
    String instanceId = searchInstance.getId();

    SearchInstances mockSearchResponse = new SearchInstances()
      .addInstancesItem(searchInstance)
      .totalRecords(1);

    wireMockServer.stubFor(WireMock.get(urlPathMatching(SEARCH_INSTANCES_MOD_SEARCH_URL))
      .withQueryParam("query", equalTo("id==" + instanceId))
      .withQueryParam("expandAll", equalTo("true"))
      .willReturn(jsonResponse(mockSearchResponse, HttpStatus.SC_OK)));

    Instance instance = new Instance().id(instanceId).editions(Set.of("1st", "2st"));
    Instances instances = new Instances().instances(List.of(instance));
    createStubForGetByIds(INSTANCE_STORAGE_URL, TENANT_ID_CONSORTIUM, instances);

    // mock items

    List<Item> itemsInConsortium = buildItems(searchItemsInConsortium);
    List<Item> itemsInCollege = buildItems(searchItemsInCollege);

    Items mockGetItemsFromConsortiumResponse = new Items()
      .items(itemsInConsortium)
      .totalRecords(searchItemsInConsortium.size());

    Items mockGetItemsFromCollegeResponse = new Items()
      .items(itemsInCollege)
      .totalRecords(searchItemsInCollege.size());

    createStubForGetByIds(ITEM_STORAGE_URL, TENANT_ID_CONSORTIUM, mockGetItemsFromConsortiumResponse);
    createStubForGetByIds(ITEM_STORAGE_URL, TENANT_ID_COLLEGE, mockGetItemsFromCollegeResponse);

    // mock holdings

    HoldingsRecords mockGetHoldingsFromConsortiumResponse = new HoldingsRecords()
      .holdingsRecords(buildHoldingsRecords(itemsInConsortium))
      .totalRecords(searchItemsInConsortium.size());

    HoldingsRecords mockGetHoldingsFromCollegeResponse = new HoldingsRecords()
      .holdingsRecords(buildHoldingsRecords(itemsInCollege))
      .totalRecords(searchItemsInCollege.size());

    createStubForGetByIds(HOLDINGS_STORAGE_URL, TENANT_ID_CONSORTIUM, mockGetHoldingsFromConsortiumResponse);
    createStubForGetByIds(HOLDINGS_STORAGE_URL, TENANT_ID_COLLEGE, mockGetHoldingsFromCollegeResponse);

    // mock locations

    Locations mockGetLocationsFromConsortiumResponse = new Locations()
      .locations(buildLocations(itemsInConsortium))
      .totalRecords(searchItemsInConsortium.size());

    Locations mockGetLocationsFromCollegeResponse = new Locations()
      .locations(buildLocations(itemsInCollege))
      .totalRecords(searchItemsInCollege.size());

    createStubForGetByIds(LOCATIONS_URL, TENANT_ID_CONSORTIUM, mockGetLocationsFromConsortiumResponse);
    createStubForGetByIds(LOCATIONS_URL, TENANT_ID_COLLEGE, mockGetLocationsFromCollegeResponse);

    // mock service points

    ServicePoints mockGetServicePointsFromConsortiumResponse = new ServicePoints()
      .servicepoints(buildServicePoints(itemsInConsortium))
      .totalRecords(itemsInConsortium.size());

    ServicePoints mockGetServicePointsFromCollegeResponse = new ServicePoints()
      .servicepoints(buildServicePoints(itemsInCollege))
      .totalRecords(itemsInCollege.size());

    createStubForGetByIds(SERVICE_POINTS_URL, TENANT_ID_CONSORTIUM, mockGetServicePointsFromConsortiumResponse);
    createStubForGetByIds(SERVICE_POINTS_URL, TENANT_ID_COLLEGE, mockGetServicePointsFromCollegeResponse);

    // mock material types

    MaterialTypes mockGetMaterialTypesFromConsortiumResponse = new MaterialTypes()
      .mtypes(buildMaterialTypes(itemsInConsortium))
      .totalRecords(itemsInConsortium.size());

    MaterialTypes mockGetMaterialTypesFromCollegeResponse = new MaterialTypes()
      .mtypes(buildMaterialTypes(itemsInCollege))
      .totalRecords(itemsInCollege.size());

    createStubForGetByIds(MATERIAL_TYPES_URL, TENANT_ID_CONSORTIUM, mockGetMaterialTypesFromConsortiumResponse);
    createStubForGetByIds(MATERIAL_TYPES_URL, TENANT_ID_COLLEGE, mockGetMaterialTypesFromCollegeResponse);

    // mock loan types

    LoanTypes mockLoanTypeResponse = new LoanTypes()
      .loantypes(List.of(
        new LoanType().id(PERMANENT_LOAN_TYPE_ID).name("permanent loan type"),
        new LoanType().id(TEMPORARY_LOAN_TYPE_ID).name("temporary loan type")))
      .totalRecords(2);

    createStubForGetByIds(LOAN_TYPES_URL, TENANT_ID_COLLEGE, mockLoanTypeResponse);
    createStubForGetByIds(LOAN_TYPES_URL, TENANT_ID_CONSORTIUM, mockLoanTypeResponse);

    mockMvc.perform(
      get(SEARCH_INSTANCES_URL)
        .queryParam("query", "id==" + instanceId)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id", is(instanceId)))
      .andExpect(jsonPath("$.holdings", hasSize(2)))
      .andExpect(jsonPath("$.holdings[?(@.tenantId == 'consortium')]", hasSize(1)))
      .andExpect(jsonPath("$.holdings[?(@.tenantId == 'college')]", hasSize(1)))
      .andExpect(jsonPath("$.holdings[?(@.tenantId == 'consortium')].id",
        containsInAnyOrder(searchHoldingInConsortium.getId())))
      .andExpect(jsonPath("$.holdings[?(@.tenantId == 'college')].id",
        containsInAnyOrder(searchHoldingInCollege.getId())))
      .andExpect(jsonPath("$.items", hasSize(90)))
      .andExpect(jsonPath("$.items[?(@.tenantId == 'consortium')]", hasSize(MAX_IDS_PER_QUERY)))
      .andExpect(jsonPath("$.items[?(@.tenantId == 'college')]", hasSize(10)))
      .andExpect(jsonPath("$.items[0].permanentLoanType.name", is("permanent loan type")))
      .andExpect(jsonPath("$.items[0].temporaryLoanType.name", is("temporary loan type")))
      .andExpect(jsonPath("$.editions", containsInAnyOrder("1st", "2st")));
  }

  private static void createStubForGetByIds(String url, String tenant, Object responsePayload) {
    wireMockServer.stubFor(WireMock.get(urlPathMatching(url))
      .withQueryParam("query", matching("id==(.*)"))
      .withHeader(HEADER_TENANT, equalTo(tenant))
      .willReturn(jsonResponse(asJsonString(responsePayload), HttpStatus.SC_OK)));
  }

}
