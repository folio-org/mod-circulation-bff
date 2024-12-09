package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.Collections.emptyList;
import static org.folio.circulationbff.service.BulkFetchingService.MAX_IDS_PER_QUERY;
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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.http.HttpStatus;
import org.folio.circulationbff.domain.dto.Contributor;
import org.folio.circulationbff.domain.dto.HoldingsRecord;
import org.folio.circulationbff.domain.dto.HoldingsRecords;
import org.folio.circulationbff.domain.dto.Identifier;
import org.folio.circulationbff.domain.dto.Instance;
import org.folio.circulationbff.domain.dto.Instances;
import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.ItemEffectiveCallNumberComponents;
import org.folio.circulationbff.domain.dto.ItemStatus;
import org.folio.circulationbff.domain.dto.Items;
import org.folio.circulationbff.domain.dto.Location;
import org.folio.circulationbff.domain.dto.Locations;
import org.folio.circulationbff.domain.dto.MaterialType;
import org.folio.circulationbff.domain.dto.MaterialTypes;
import org.folio.circulationbff.domain.dto.Publication;
import org.folio.circulationbff.domain.dto.SearchHolding;
import org.folio.circulationbff.domain.dto.SearchInstance;
import org.folio.circulationbff.domain.dto.SearchInstances;
import org.folio.circulationbff.domain.dto.SearchItem;
import org.folio.circulationbff.domain.dto.SearchItemEffectiveCallNumberComponents;
import org.folio.circulationbff.domain.dto.SearchItemStatus;
import org.folio.circulationbff.domain.dto.ServicePoint;
import org.folio.circulationbff.domain.dto.ServicePoints;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.MediaType;

import com.github.tomakehurst.wiremock.client.WireMock;

import lombok.SneakyThrows;

class SearchInstancesApiTest extends BaseIT {

  private static final String SEARCH_INSTANCES_URL = "/circulation-bff/requests/search-instances";
  private static final String SEARCH_INSTANCES_MOD_SEARCH_URL = "/search/instances";
  private static final String ITEM_STORAGE_URL = "/item-storage/items";
  private static final String HOLDINGS_STORAGE_URL = "/holdings-storage/holdings";
  private static final String LOCATIONS_URL = "/locations";
  private static final String SERVICE_POINTS_URL = "/service-points";
  private static final String MATERIAL_TYPES_URL = "/material-types";

  private static final String INSTANCE_STORAGE_URL = "/instance-storage/instances";
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
      .andExpect(jsonPath("$.editions", containsInAnyOrder("1st", "2st")));
  }

  private static SearchInstance buildSearchInstance(String tenantId, List<SearchItem> searchItems,
    List<SearchHolding> searchHoldings) {

    return new SearchInstance()
      .id(randomId())
      .tenantId(tenantId)
      .holdings(searchHoldings)
      .items(searchItems)
      .shared(true)
      .hrid("test_instance_hrid")
      .source("FOLIO")
      .title("test title")
      .identifiers(List.of(
        new Identifier()
          .value("identifier_value_1")
          .identifierTypeId(randomId()),
        new Identifier()
          .value("identifier_value_2")
          .identifierTypeId(randomId())))
      .contributors(List.of(
        new Contributor()
          .name("Author, One")
          .contributorNameTypeId(randomId())
          .primary(true),
        new Contributor()
          .name("Author, Two")
          .contributorNameTypeId(randomId())
          .primary(false)))
      .publication(List.of(
        new Publication()
          .publisher("publisher_1")
          .dateOfPublication("1950")
          .place("place_1"),
        new Publication()
          .publisher("publisher_1")
          .dateOfPublication("1950")
          .place("place_1")));
  }

  private static List<SearchItem> buildSearchItems(int count, String tenantId, String holdingsId) {
    return IntStream.range(0, count)
      .boxed()
      .map(idx -> buildSearchItem(idx, tenantId, holdingsId))
      .toList();
  }

  private static SearchItem buildSearchItem(int index, String tenantId, String holdingsId) {
    return new SearchItem()
      .id(randomId())
      .tenantId(tenantId)
      .holdingsRecordId(holdingsId)
      .hrid("test_item_hrid")
      .barcode("test_item_barcode_" + index)
      .effectiveLocationId(randomId())
      .status(new SearchItemStatus().name("Available"))
      .materialTypeId(randomId())
      .discoverySuppress(false)
      .effectiveCallNumberComponents(new SearchItemEffectiveCallNumberComponents()
        .callNumber("CN")
        .prefix("PFX")
        .suffix("SFX")
        .typeId(randomId()))
      .effectiveShelvingOrder("test_shelving_order");
  }

  private static SearchHolding buildSearchHolding(String tenantId) {
    return new SearchHolding()
      .id(randomId())
      .tenantId(tenantId)
      .permanentLocationId(randomId())
      .hrid("test_holdings_hrid")
      .notes(emptyList());
  }

  private static List<Item> buildItems(Collection<SearchItem> searchItems) {
    return searchItems.stream()
      .map(SearchInstancesApiTest::buildItem)
      .toList();
  }

  private static Item buildItem(SearchItem searchItem) {
    return new Item()
      .id(searchItem.getId())
      .barcode(searchItem.getBarcode())
      .holdingsRecordId(searchItem.getHoldingsRecordId())
      .enumeration("test_enumeration")
      .chronology("test_chronology")
      .displaySummary("test_display_summary")
      .volume("test_volume")
      .copyNumber("test_item_copy_number")
      .status(new ItemStatus()
        .name(ItemStatus.NameEnum.AVAILABLE)
        .date(new Date()))
      .inTransitDestinationServicePointId(randomId())
      .effectiveCallNumberComponents(new ItemEffectiveCallNumberComponents()
        .callNumber(searchItem.getEffectiveCallNumberComponents().getCallNumber())
        .prefix(searchItem.getEffectiveCallNumberComponents().getPrefix())
        .suffix(searchItem.getEffectiveCallNumberComponents().getSuffix()))
      .effectiveLocationId(randomId())
      .materialTypeId(randomId());
  }

  private static List<HoldingsRecord> buildHoldingsRecords(Collection<Item> items) {
    return items.stream()
      .map(Item::getHoldingsRecordId)
      .distinct()
      .map(SearchInstancesApiTest::buildHoldingsRecord)
      .toList();
  }

  private static HoldingsRecord buildHoldingsRecord(String id) {
    return new HoldingsRecord()
      .id(id)
      .copyNumber("test_holding_copy_number");
  }

  private static List<Location> buildLocations(Collection<Item> items) {
    return items.stream()
      .map(Item::getEffectiveLocationId)
      .distinct()
      .map(SearchInstancesApiTest::buildLocation)
      .toList();
  }

  private static Location buildLocation(String id) {
    return new Location()
      .id(id)
      .name("test_location");
  }

  private static List<ServicePoint> buildServicePoints(Collection<Item> items) {
    return items.stream()
      .map(Item::getInTransitDestinationServicePointId)
      .distinct()
      .map(SearchInstancesApiTest::buildServicePoint)
      .toList();
  }

  private static ServicePoint buildServicePoint(String id) {
    return new ServicePoint()
      .id(id)
      .name("test_service_point");
  }

  private static List<MaterialType> buildMaterialTypes(Collection<Item> items) {
    return items.stream()
      .map(Item::getMaterialTypeId)
      .map(SearchInstancesApiTest::buildMaterialType)
      .toList();
  }

  private static MaterialType buildMaterialType(String id) {
    return new MaterialType()
      .id(id)
      .name("test_material_type");
  }

  private static void createStubForGetByIds(String url, String tenant, Object responsePayload) {
    wireMockServer.stubFor(WireMock.get(urlPathMatching(url))
      .withQueryParam("query", matching("id==(.*)"))
      .withHeader(HEADER_TENANT, equalTo(tenant))
      .willReturn(jsonResponse(asJsonString(responsePayload), HttpStatus.SC_OK)));
  }

}
