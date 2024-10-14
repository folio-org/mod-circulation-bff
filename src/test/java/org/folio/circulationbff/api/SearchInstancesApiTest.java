package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.http.HttpStatus;
import org.folio.circulationbff.domain.dto.Contributor;
import org.folio.circulationbff.domain.dto.HoldingsRecord;
import org.folio.circulationbff.domain.dto.HoldingsRecords;
import org.folio.circulationbff.domain.dto.Identifier;
import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.ItemEffectiveCallNumberComponents;
import org.folio.circulationbff.domain.dto.ItemStatus;
import org.folio.circulationbff.domain.dto.Items;
import org.folio.circulationbff.domain.dto.Location;
import org.folio.circulationbff.domain.dto.Locations;
import org.folio.circulationbff.domain.dto.MaterialType;
import org.folio.circulationbff.domain.dto.MaterialTypes;
import org.folio.circulationbff.domain.dto.Metadata;
import org.folio.circulationbff.domain.dto.Publication;
import org.folio.circulationbff.domain.dto.SearchHolding;
import org.folio.circulationbff.domain.dto.SearchInstance;
import org.folio.circulationbff.domain.dto.SearchInstances;
import org.folio.circulationbff.domain.dto.SearchItem;
import org.folio.circulationbff.domain.dto.SearchItemEffectiveCallNumberComponents;
import org.folio.circulationbff.domain.dto.SearchItemStatus;
import org.folio.circulationbff.domain.dto.ServicePoint;
import org.folio.circulationbff.domain.dto.ServicePoints;
import org.folio.circulationbff.domain.dto.Tags;
import org.junit.jupiter.api.Test;
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
      .andExpect(jsonPath("instances", is(emptyIterable())))
      .andExpect(jsonPath("totalRecords", is(0)));
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
      ));

    wireMockServer.stubFor(WireMock.get(urlPathMatching(SEARCH_INSTANCES_MOD_SEARCH_URL))
      .withQueryParam("query", equalTo("id==" + instanceId))
      .withQueryParam("expandAll", equalTo("true"))
      .willReturn(jsonResponse(asJsonString(mockSearchInstancesResponse), HttpStatus.SC_OK)));

    mockMvc.perform(
        get(SEARCH_INSTANCES_URL)
          .queryParam("query", "id==" + instanceId)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("instances", hasSize(1)))
      .andExpect(jsonPath("instances[0].id", is(instanceId)))
      .andExpect(jsonPath("instances[0].items", emptyIterable()))
      .andExpect(jsonPath("totalRecords", is(1)));
  }

  @Test
  @SneakyThrows
  void searchInstancesReturnsOkStatus() {
    SearchHolding searchHolding = buildSearchHolding(TENANT_ID_CONSORTIUM);
    List<SearchItem> searchItemsInConsortium = buildSearchItems(85, TENANT_ID_CONSORTIUM);
    List<SearchItem> searchItemsInCollege = buildSearchItems(15, TENANT_ID_COLLEGE);
    List<SearchItem> allSearchItems = Stream.concat(searchItemsInConsortium.stream(),
        searchItemsInCollege.stream())
      .toList();

    List<Item> itemsInConsortium = buildItems(searchItemsInConsortium);
    List<Item> itemsInCollege = buildItems(searchItemsInCollege);

    SearchInstance searchInstance = buildSearchInstance(TENANT_ID_CONSORTIUM, allSearchItems,
      List.of(searchHolding));
    String instanceId = searchInstance.getId();

    // mock instance search

    SearchInstances mockSearchResponse = new SearchInstances()
      .addInstancesItem(searchInstance)
      .totalRecords(1);

    wireMockServer.stubFor(WireMock.get(urlPathMatching(SEARCH_INSTANCES_MOD_SEARCH_URL))
      .withQueryParam("query", equalTo("id==" + instanceId))
      .withQueryParam("expandAll", equalTo("true"))
      .willReturn(jsonResponse(mockSearchResponse, HttpStatus.SC_OK)));

    // mock items

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
      .andExpect(jsonPath("instances[0].id", is(instanceId)))
      .andExpect(jsonPath("totalRecords", is(1)));
  }

  private static SearchInstance buildSearchInstance(String tenantId, List<SearchItem> searchItems,
    List<SearchHolding> searchHoldings) {

    return new SearchInstance()
      .id(randomId())
      .tenantId(tenantId)
      .shared(true)
      .hrid("test_instance_hrid")
      .source("FOLIO")
      .statisticalCodeIds(emptyList())
      .title("test title")
      .series(emptyList())
      .alternativeTitles(emptyList())
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
      .subjects(emptyList())
      .instanceTypeId(randomId())
      .instanceFormatIds(emptyList())
      .languages(emptyList())
      .metadata(new Metadata()
        .createdDate(new Date().toString())
        .createdByUserId(randomId())
        .updatedDate(new Date().toString())
        .updatedByUserId(randomId()))
      .administrativeNotes(emptyList())
      .natureOfContentTermIds(emptyList())
      .publication(List.of(
        new Publication()
          .publisher("publisher_1")
          .dateOfPublication("1950")
          .place("place_1"),
        new Publication()
          .publisher("publisher_1")
          .dateOfPublication("1950")
          .place("place_1")))
      .staffSuppress(false)
      .discoverySuppress(false)
      .isBoundWith(false)
      .tags(new Tags())
      .classifications(emptyList())
      .electronicAccess(emptyList())
      .notes(emptyList())
      .holdings(searchHoldings)
      .items(searchItems);
  }

  private static List<SearchItem> buildSearchItems(int count, String tenantId) {
    return IntStream.range(0, count)
      .boxed()
      .map(idx -> buildSearchItem(idx, tenantId))
      .toList();
  }

  private static SearchItem buildSearchItem(int index, String tenantId) {
    return new SearchItem()
      .id(randomId())
      .tenantId(tenantId)
      .holdingsRecordId(randomId())
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
      .effectiveShelvingOrder("test_shelving_order")
      .itemLevelCallNumberTypeId(randomId())
      .tags(new Tags())
      .electronicAccess(emptyList())
      .administrativeNotes(emptyList())
      .notes(emptyList())
      .statisticalCodeIds(emptyList())
      .circulationNotes(emptyList())
      .metadata(new Metadata()
        .createdDate(new Date().toString())
        .createdByUserId(randomId())
        .updatedDate(new Date().toString())
        .updatedByUserId(randomId()));
  }

  private static SearchHolding buildSearchHolding(String tenantId) {
    return new SearchHolding()
      .id(randomId())
      .tenantId(tenantId)
      .permanentLocationId(randomId())
      .discoverySuppress(false)
      .hrid("test_holdings_hrid")
      .sourceId(randomId())
      .formerIds(emptyList())
      .statisticalCodeIds(emptyList())
      .holdingsTypeId(randomId())
      .electronicAccess(emptyList())
      .administrativeNotes(emptyList())
      .notes(emptyList())
      .metadata(new Metadata()
        .createdDate(new Date().toString())
        .createdByUserId(randomId())
        .updatedDate(new Date().toString())
        .updatedByUserId(randomId()));
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
      .map(SearchInstancesApiTest::buildHoldingsRecord)
      .toList();
  }

  private static HoldingsRecord buildHoldingsRecord(Item item) {
    return new HoldingsRecord()
      .id(item.getHoldingsRecordId())
      .copyNumber("test_holding_copy_number");
  }

  private static List<Location> buildLocations(Collection<Item> items) {
    return items.stream()
      .map(SearchInstancesApiTest::buildLocation)
      .toList();
  }

  private static Location buildLocation(Item item) {
    return new Location()
      .id(item.getEffectiveLocationId())
      .name("test_location");
  }

  private static List<ServicePoint> buildServicePoints(Collection<Item> items) {
    return items.stream()
      .map(SearchInstancesApiTest::buildServicePoint)
      .toList();
  }

  private static ServicePoint buildServicePoint(Item item) {
    return new ServicePoint()
      .id(item.getInTransitDestinationServicePointId())
      .name("test_service_point");
  }

  private static List<MaterialType> buildMaterialTypes(Collection<Item> items) {
    return items.stream()
      .map(SearchInstancesApiTest::buildMaterialType)
      .toList();
  }

  private static MaterialType buildMaterialType(Item item) {
    return new MaterialType()
      .id(item.getMaterialTypeId())
      .name("test_material_type");
  }

  private static void createStubForGetByIds(String url, String tenant, Object responsePayload) {
    wireMockServer.stubFor(WireMock.get(urlPathMatching(url))
      .withQueryParam("query", matching("id==(.*)"))
      .withHeader(HEADER_TENANT, equalTo(tenant))
      .willReturn(jsonResponse(asJsonString(responsePayload), HttpStatus.SC_OK)));
  }

}
