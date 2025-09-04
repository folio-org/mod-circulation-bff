package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.folio.circulationbff.util.ApiEndpointURL.CIRCULATION_BFF_INVENTORY_ITEMS_URL;
import static org.folio.circulationbff.util.ApiEndpointURL.INVENTORY_ITEMS_URL;
import static org.folio.circulationbff.util.ApiEndpointURL.SEARCH_INSTANCES_MOD_SEARCH_URL;
import static org.folio.circulationbff.util.MockHelper.buildInventoryItem;
import static org.folio.circulationbff.util.MockHelper.buildSearchHolding;
import static org.folio.circulationbff.util.MockHelper.buildSearchInstance;
import static org.folio.circulationbff.util.MockHelper.buildSearchItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;

import org.apache.http.HttpStatus;
import org.folio.circulationbff.domain.dto.SearchHolding;
import org.folio.circulationbff.domain.dto.SearchInstance;
import org.folio.circulationbff.domain.dto.SearchInstances;
import org.folio.circulationbff.domain.dto.SearchItem;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.github.tomakehurst.wiremock.client.WireMock;

class InventoryItemsApiTest extends BaseIT {

  @Test
  void getInventoryItemsReturnsEmptyList() throws Exception {
    wireMockServer.stubFor(WireMock.get(urlPathMatching(SEARCH_INSTANCES_MOD_SEARCH_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(new SearchInstances()
        .totalRecords(0), HttpStatus.SC_OK)));

    mockMvc.perform(get(CIRCULATION_BFF_INVENTORY_ITEMS_URL)
        .queryParam("limit", "100")
        .queryParam("offset", "30")
        .queryParam("query", "barcode==test-barcode")
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.items", hasSize(0)))
      .andExpect(jsonPath("$.totalRecords", is(0)));
  }

  @Test
  void getInventoryItemsReturnsItems() throws Exception {
    SearchHolding searchHoldingInConsortium = buildSearchHolding(TENANT_ID_CONSORTIUM);
    SearchHolding searchHoldingInCollege = buildSearchHolding(TENANT_ID_COLLEGE);

    List<SearchItem> searchItemsInConsortium = buildSearchItems(0, TENANT_ID_CONSORTIUM,
      searchHoldingInConsortium.getId());
    List<SearchItem> searchItemsInCollege = buildSearchItems(10, TENANT_ID_COLLEGE,
      searchHoldingInCollege.getId());
    List<SearchItem> allSearchItems = Stream.concat(searchItemsInConsortium.stream(),
        searchItemsInCollege.stream())
      .toList();

    SearchInstance searchInstance = buildSearchInstance(TENANT_ID_CONSORTIUM, allSearchItems,
      List.of(searchHoldingInConsortium, searchHoldingInCollege));

    SearchInstances mockSearchResponse = new SearchInstances()
      .addInstancesItem(searchInstance)
      .totalRecords(1);

    var expectedItem = searchItemsInCollege.get(3);
    var expectedItemId = expectedItem.getId();
    var expectedItemBarcode = expectedItem.getBarcode();
    wireMockServer.stubFor(WireMock.get(urlPathMatching(SEARCH_INSTANCES_MOD_SEARCH_URL))
      .withQueryParam("query", equalTo("items.barcode==" + expectedItemBarcode))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(mockSearchResponse, HttpStatus.SC_OK)));

    wireMockServer.stubFor(WireMock.get(urlPathMatching(INVENTORY_ITEMS_URL + "/" + expectedItemId))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(jsonResponse(buildInventoryItem(expectedItemId, expectedItemBarcode),
        HttpStatus.SC_OK)));

    mockMvc.perform(get(CIRCULATION_BFF_INVENTORY_ITEMS_URL)
        .queryParam("limit", "100")
        .queryParam("offset", "30")
        .queryParam("query", "barcode==" + expectedItemBarcode)
        .headers(defaultHeaders())
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.items", hasSize(1)))
      .andExpect(jsonPath("$.items[0].barcode", is(expectedItemBarcode)))
      .andExpect(jsonPath("$.totalRecords", is(1)));

    wireMockServer.verify(getRequestedFor(urlPathMatching(
      INVENTORY_ITEMS_URL + "/" + expectedItemId))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE)));
  }
}

