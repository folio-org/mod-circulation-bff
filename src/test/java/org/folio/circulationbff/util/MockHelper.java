package org.folio.circulationbff.util;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.util.Collections.emptyList;
import static org.apache.http.HttpStatus.SC_OK;
import static org.folio.circulationbff.api.BaseIT.HEADER_TENANT;
import static org.folio.circulationbff.api.BaseIT.TENANT_ID_CONSORTIUM;
import static org.folio.circulationbff.api.BaseIT.asJsonString;
import static org.folio.circulationbff.api.BaseIT.randomId;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.http.HttpStatus;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.AllowedServicePoints1Inner;
import org.folio.circulationbff.domain.dto.CirculationSettings;
import org.folio.circulationbff.domain.dto.CirculationSettingsResponse;
import org.folio.circulationbff.domain.dto.CirculationSettingsValue;
import org.folio.circulationbff.domain.dto.Contributor;
import org.folio.circulationbff.domain.dto.HoldingsRecord;
import org.folio.circulationbff.domain.dto.Identifier;
import org.folio.circulationbff.domain.dto.InventoryItem;
import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.ItemEffectiveCallNumberComponents;
import org.folio.circulationbff.domain.dto.ItemStatus;
import org.folio.circulationbff.domain.dto.Location;
import org.folio.circulationbff.domain.dto.MaterialType;
import org.folio.circulationbff.domain.dto.PickSlipCollection;
import org.folio.circulationbff.domain.dto.Publication;
import org.folio.circulationbff.domain.dto.SearchHolding;
import org.folio.circulationbff.domain.dto.SearchInstance;
import org.folio.circulationbff.domain.dto.SearchItem;
import org.folio.circulationbff.domain.dto.SearchItemEffectiveCallNumberComponents;
import org.folio.circulationbff.domain.dto.SearchItemStatus;
import org.folio.circulationbff.domain.dto.SearchSlipCollection;
import org.folio.circulationbff.domain.dto.ServicePoint;
import org.folio.circulationbff.domain.dto.TlrSettings;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.folio.circulationbff.domain.dto.UserTenantCollection;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

public class MockHelper {
  private static final String CIRCULATION_SETTINGS_URL = "/circulation/settings";
  private static final String TLR_SETTINGS_URL = "/tlr/settings";
  private static final String USER_TENANTS_URL = "/user-tenants";
  public static final String TLR_ALLOWED_SERVICE_POINT_URL = "/tlr/allowed-service-points";
  private static final String SEARCH_INSTANCES_MOD_SEARCH_URL = "/search/instances";
  private static final String PERMANENT_LOAN_TYPE_ID = "22fa71d319-997b-4a60-8cfd-20fdf57efa14";
  private static final String TEMPORARY_LOAN_TYPE_ID = "2286d4aed0-c76b-4907-983f-1327dfb4b12d";

  private WireMockServer wireMockServer;

  public MockHelper(WireMockServer wireMockServer) {
    this.wireMockServer = wireMockServer;
  }

  public void mockEcsTlrSettings(boolean enabled) {
    TlrSettings tlrSettings = new TlrSettings().ecsTlrFeatureEnabled(enabled);
    wireMockServer.stubFor(WireMock.get(urlMatching(TLR_SETTINGS_URL))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(asJsonString(tlrSettings), SC_OK)));
  }

  public void mockEcsTlrCirculationSettings(boolean enabled, String tenantId) {
    var circulationSettingsResponse = new CirculationSettingsResponse();
    circulationSettingsResponse.setTotalRecords(1);
    circulationSettingsResponse.setCirculationSettings(List.of(
      new CirculationSettings()
        .name("ecsTlrFeature")
        .value(new CirculationSettingsValue().enabled(enabled))
    ));
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(CIRCULATION_SETTINGS_URL))
      .withQueryParam("query", equalTo("name=ecsTlrFeature"))
      .withHeader(HEADER_TENANT, equalTo(tenantId))
      .willReturn(jsonResponse(asJsonString(circulationSettingsResponse),
        SC_OK)));
  }

  public void mockSearchSlips(SearchSlipCollection searchSlips, UrlPathPattern externalUrl,
    String requesterTenantId) {

    wireMockServer.stubFor(WireMock.get(externalUrl)
      .withHeader(HEADER_TENANT, equalTo(requesterTenantId))
      .willReturn(jsonResponse(searchSlips, HttpStatus.SC_OK)));
  }

  public void mockPickSlips(PickSlipCollection pickSlips, UrlPathPattern externalUrl,
    String requesterTenantId) {

    wireMockServer.stubFor(WireMock.get(externalUrl)
      .withHeader(HEADER_TENANT, equalTo(requesterTenantId))
      .willReturn(jsonResponse(pickSlips, HttpStatus.SC_OK)));
  }

  public void mockUserTenants(UserTenantCollection userTenants, String requesterTenantId) {
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(USER_TENANTS_URL))
      .withHeader(HEADER_TENANT, equalTo(requesterTenantId))
      .withQueryParam("limit", matching("\\d*"))
      .willReturn(jsonResponse(asJsonString(userTenants), SC_OK)));
  }

  public void mockEcsTlrSettings(TlrSettings tlrSettings, String requesterTenantId) {
    wireMockServer.stubFor(WireMock.get(urlMatching(TLR_SETTINGS_URL))
      .withHeader(HEADER_TENANT, equalTo(requesterTenantId))
      .willReturn(jsonResponse(asJsonString(tlrSettings), SC_OK)));
  }

  public void mockUserTenants(UserTenant userTenant, String requestTenant) {
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(USER_TENANTS_URL))
      .withQueryParam("limit", matching("\\d*"))
      .withHeader(HEADER_TENANT, equalTo(requestTenant))
      .willReturn(jsonResponse(asJsonString(new UserTenantCollection().addUserTenantsItem(userTenant)),
        SC_OK)));
  }

  public void mockAllowedServicePoints(String requestTenant) {
    var allowedSpResponseConsortium = new AllowedServicePoints();
    allowedSpResponseConsortium.setHold(Set.of(
      buildAllowedServicePoint("SP_consortium_1"),
      buildAllowedServicePoint("SP_consortium_2")));
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(TLR_ALLOWED_SERVICE_POINT_URL))
      .withHeader(HEADER_TENANT, equalTo(requestTenant))
      .willReturn(jsonResponse(asJsonString(allowedSpResponseConsortium), SC_OK)));
  }

  public AllowedServicePoints1Inner buildAllowedServicePoint(String name) {
    return new AllowedServicePoints1Inner()
      .id(randomId())
      .discoveryDisplayName(name)
      .name(name);
  }

  public static SearchInstance buildSearchInstance(String tenantId, List<SearchItem> searchItems,
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

  public static List<SearchItem> buildSearchItems(int count, String tenantId, String holdingsId) {
    return IntStream.range(0, count)
      .boxed()
      .map(idx -> buildSearchItem(idx, tenantId, holdingsId))
      .toList();
  }

  public static SearchItem buildSearchItem(int index, String tenantId, String holdingsId) {
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

  public static SearchHolding buildSearchHolding(String tenantId) {
    return new SearchHolding()
      .id(randomId())
      .tenantId(tenantId)
      .permanentLocationId(randomId())
      .hrid("test_holdings_hrid")
      .notes(emptyList());
  }

  public static List<Item> buildItems(Collection<SearchItem> searchItems) {
    return searchItems.stream()
      .map(MockHelper::buildItem)
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
      .materialTypeId(randomId())
      .permanentLoanTypeId(PERMANENT_LOAN_TYPE_ID)
      .temporaryLoanTypeId(TEMPORARY_LOAN_TYPE_ID);
  }

  public static List<HoldingsRecord> buildHoldingsRecords(Collection<Item> items) {
    return items.stream()
      .map(Item::getHoldingsRecordId)
      .distinct()
      .map(MockHelper::buildHoldingsRecord)
      .toList();
  }

  public static HoldingsRecord buildHoldingsRecord(String id) {
    return new HoldingsRecord()
      .id(id)
      .copyNumber("test_holding_copy_number");
  }

  public static List<Location> buildLocations(Collection<Item> items) {
    return items.stream()
      .map(Item::getEffectiveLocationId)
      .distinct()
      .map(MockHelper::buildLocation)
      .toList();
  }

  public static Location buildLocation(String id) {
    return new Location()
      .id(id)
      .name("test_location");
  }

  public static List<ServicePoint> buildServicePoints(Collection<Item> items) {
    return items.stream()
      .map(Item::getInTransitDestinationServicePointId)
      .distinct()
      .map(MockHelper::buildServicePoint)
      .toList();
  }

  public static ServicePoint buildServicePoint(String id) {
    return new ServicePoint()
      .id(id)
      .name("test_service_point");
  }

  public static List<MaterialType> buildMaterialTypes(Collection<Item> items) {
    return items.stream()
      .map(Item::getMaterialTypeId)
      .map(MockHelper::buildMaterialType)
      .toList();
  }

  public static MaterialType buildMaterialType(String id) {
    return new MaterialType()
      .id(id)
      .name("test_material_type");
  }

  public static InventoryItem buildInventoryItem(String id, String barcode) {
    return new InventoryItem()
      .id(id)
      .barcode(barcode);
  }

}
