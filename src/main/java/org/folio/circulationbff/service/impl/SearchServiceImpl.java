package org.folio.circulationbff.service.impl;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.folio.circulationbff.client.feign.HoldingsStorageClient;
import org.folio.circulationbff.client.feign.InstanceStorageClient;
import org.folio.circulationbff.client.feign.ItemStorageClient;
import org.folio.circulationbff.client.feign.LocationClient;
import org.folio.circulationbff.client.feign.MaterialTypeClient;
import org.folio.circulationbff.client.feign.SearchClient;
import org.folio.circulationbff.client.feign.ServicePointClient;
import org.folio.circulationbff.domain.dto.BffSearchInstance;
import org.folio.circulationbff.domain.dto.BffSearchItem;
import org.folio.circulationbff.domain.dto.BffSearchItemCallNumberComponents;
import org.folio.circulationbff.domain.dto.BffSearchItemInTransitDestinationServicePoint;
import org.folio.circulationbff.domain.dto.BffSearchItemLocation;
import org.folio.circulationbff.domain.dto.BffSearchItemMaterialType;
import org.folio.circulationbff.domain.dto.BffSearchItemStatus;
import org.folio.circulationbff.domain.dto.Contributor;
import org.folio.circulationbff.domain.dto.HoldingsRecord;
import org.folio.circulationbff.domain.dto.HoldingsRecords;
import org.folio.circulationbff.domain.dto.Instance;
import org.folio.circulationbff.domain.dto.Instances;
import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.ItemEffectiveCallNumberComponents;
import org.folio.circulationbff.domain.dto.Items;
import org.folio.circulationbff.domain.dto.Location;
import org.folio.circulationbff.domain.dto.Locations;
import org.folio.circulationbff.domain.dto.MaterialType;
import org.folio.circulationbff.domain.dto.MaterialTypes;
import org.folio.circulationbff.domain.dto.SearchInstance;
import org.folio.circulationbff.domain.dto.SearchInstances;
import org.folio.circulationbff.domain.dto.SearchItem;
import org.folio.circulationbff.domain.dto.ServicePoint;
import org.folio.circulationbff.domain.dto.ServicePoints;
import org.folio.circulationbff.domain.mapping.SearchInstanceMapper;
import org.folio.circulationbff.service.BulkFetchingService;
import org.folio.circulationbff.service.SearchService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

  private final ItemStorageClient itemStorageClient;
  private final HoldingsStorageClient holdingsStorageClient;
  private final LocationClient locationClient;
  private final MaterialTypeClient materialTypeClient;
  private final ServicePointClient servicePointClient;
  private final SearchClient searchClient;
  private final InstanceStorageClient instanceStorageClient;
  private final SystemUserScopedExecutionService executionService;
  private final BulkFetchingService fetchingService;
  private final SearchInstanceMapper searchInstanceMapper;

  @Override
  public SearchInstance findInstanceByItemId(String itemId) {
    log.info("findInstanceByItemId:: itemId {}", itemId);
    String query = "items.id==" + itemId;
    SearchInstances searchResult = searchClient.findInstances(query, true);
    if (CollectionUtils.isEmpty(searchResult.getInstances())) {
      return null;
    }
    return searchResult.getInstances().get(0);
  }

  @Override
  public SearchInstance findInstanceByItemBarcode(String itemBarcode) {
    log.info("findInstanceByItemBarcode:: itemBarcode {}", itemBarcode);
    String query = "items.barcode==" + itemBarcode;
    SearchInstances searchResult = searchClient.findInstances(query, true);
    if (CollectionUtils.isEmpty(searchResult.getInstances())) {
      log.info("findInstanceByItemBarcode:: found nothing by itemBarcode {}", itemBarcode);
      return null;
    }

    return searchResult.getInstances().stream()
      .findFirst()
      .orElse(null);
  }

  @Override
  public Collection<BffSearchInstance> findInstances(String query) {
    log.info("findInstances:: searching instances by query: {}", query);
    final SearchInstances searchResult = searchClient.findInstances(query, true);
    final List<SearchInstance> searchInstances = searchResult.getInstances();

    if (searchInstances.isEmpty()) {
      log.info("findInstances:: no instances found");
      return emptyList();
    }
    log.info("findInstances:: {} instances found", searchInstances::size);

    Collection<BffSearchInstance> bffSearchInstances =
      fetchEditions(buildBffSearchInstances(searchInstances));
    log.info("findInstances:: successfully built {} instances", bffSearchInstances::size);

    return bffSearchInstances;
  }

  private Collection<ItemContext> fetchItemDetails(Collection<SearchInstance> searchInstances) {
    log.info("fetchItemDetails:: fetching item details for {} instances", searchInstances::size);
    Map<String, List<SearchItem>> itemsByTenant =  searchInstances.stream()
      .map(SearchInstance::getItems)
      .flatMap(Collection::stream)
      .collect(groupingBy(SearchItem::getTenantId));

    if (itemsByTenant.isEmpty()) {
      log.info("fetchItemDetails:: no items found, doing nothing");
      return emptyList();
    }

    log.info("fetchItemDetails:: fetching item details from {} tenants: {}", itemsByTenant::size,
      itemsByTenant::keySet);

    return itemsByTenant.entrySet()
      .stream()
      .map(entry -> fetchItemDetails(entry.getKey(), entry.getValue()))
      .flatMap(Collection::stream)
      .toList();
  }

  private Collection<ItemContext> fetchItemDetails(String tenantId, Collection<SearchItem> items) {
    log.info("fetchItemDetails:: fetching details for {} items in tenant {}", items.size(), tenantId);
    return executionService.executeSystemUserScoped(tenantId, () -> buildItemContexts(items));
  }

  private Collection<BffSearchInstance> fetchEditions(Collection<BffSearchInstance> bffSearchInstances) {
    return bffSearchInstances.stream()
      .collect(Collectors.groupingBy(BffSearchInstance::getTenantId))
      .entrySet()
      .stream()
      .flatMap(entry -> fetchEditions(entry.getKey(), entry.getValue()).stream())
      .toList();
  }

  private Collection<BffSearchInstance> fetchEditions(String tenantId,
    Collection<BffSearchInstance> searchInstances) {
    log.info("fetchItemDetails:: fetching details for {} items in tenant {}", searchInstances.size(), tenantId);
    return executionService.executeSystemUserScoped(tenantId,
      () -> updateSearchInstanceEditions(searchInstances));
  }

  private Collection<BffSearchInstance> updateSearchInstanceEditions(Collection<BffSearchInstance> searchInstances) {
    Map<String, Instance> instanceMap = fetchInstances(
      searchInstances.stream()
        .map(BffSearchInstance::getId)
        .collect(Collectors.toSet())
    ).stream()
      .collect(Collectors.toMap(Instance::getId, Function.identity()));

    searchInstances.forEach(searchInstance -> {
      Instance instance = instanceMap.get(searchInstance.getId());
      if (instance != null) {
        searchInstance.setEditions(instance.getEditions());
      }
    });

    return searchInstances;
  }

  private Collection<Instance> fetchInstances(Collection<String> ids) {
    log.info("fetchInstances: fetching {} instances", ids::size);
    return fetchingService.fetch(instanceStorageClient, ids, Instances::getInstances);
  }

  private Collection<ItemContext> buildItemContexts(Collection<SearchItem> searchItems) {
    Set<String> itemIds = searchItems.stream()
      .map(SearchItem::getId)
      .collect(toSet());

    Collection<Item> items = fetchItems(itemIds);
    Map<String, HoldingsRecord> holdingsRecordsById = fetchHoldingsRecords(items);
    Map<String, Location> locationsById = fetchLocations(items);
    Map<String, ServicePoint> servicePointsById = fetchServicePoints(items);
    Map<String, MaterialType> materialTypesById = fetchMaterialTypes(items);
    Map<String, String> itemIdToTenantId = searchItems.stream()
      .collect(toMap(SearchItem::getId, SearchItem::getTenantId));

    return items.stream()
      .map(item -> new ItemContext(item,
        itemIdToTenantId.get(item.getId()),
        holdingsRecordsById.get(item.getHoldingsRecordId()),
        locationsById.get(item.getEffectiveLocationId()),
        materialTypesById.get(item.getMaterialTypeId()),
        servicePointsById.get(item.getInTransitDestinationServicePointId())))
      .toList();
  }

  private Collection<Item> fetchItems(Collection<String> ids) {
    log.info("fetchItems: fetching {} items", ids::size);
    return fetchingService.fetch(itemStorageClient, ids, Items::getItems);
  }

  private Map<String, HoldingsRecord> fetchHoldingsRecords(Collection<Item> items) {
    Collection<String> ids = extractUniqueValues(items, Item::getHoldingsRecordId);
    log.info("fetchHoldingsRecords: fetching {} holdingsRecords", ids::size);
    return fetchingService.fetch(holdingsStorageClient, ids, HoldingsRecords::getHoldingsRecords,
      HoldingsRecord::getId);
  }

  private Map<String, Location> fetchLocations(Collection<Item> items) {
    Collection<String> ids = extractUniqueValues(items, Item::getEffectiveLocationId);
    log.info("fetchLocations: fetching {} locations", ids::size);
    return fetchingService.fetch(locationClient, ids, Locations::getLocations, Location::getId);
  }

  private Map<String, ServicePoint> fetchServicePoints(Collection<Item> items) {
    Collection<String> ids = extractUniqueValues(items, Item::getInTransitDestinationServicePointId);
    log.info("fetchServicePoints: fetching {} service points", ids::size);
    return fetchingService.fetch(servicePointClient, ids, ServicePoints::getServicepoints,
      ServicePoint::getId);
  }

  private Map<String, MaterialType> fetchMaterialTypes(Collection<Item> items) {
    Collection<String> ids = extractUniqueValues(items, Item::getMaterialTypeId);
    log.info("fetchMaterialTypes: fetching {} material types", ids::size);
    return fetchingService.fetch(materialTypeClient, ids, MaterialTypes::getMtypes,
      MaterialType::getId);
  }

  private Collection<BffSearchInstance> buildBffSearchInstances(
    Collection<SearchInstance> searchInstances) {

    Collection<ItemContext> itemContexts = fetchItemDetails(searchInstances);
    log.info("buildBffSearchInstances:: successfully built contexts for {} items", itemContexts::size);

    return searchInstances.stream()
      .map(searchInstance -> buildBffSearchInstance(searchInstance, itemContexts))
      .toList();
  }

  private BffSearchInstance buildBffSearchInstance(SearchInstance searchInstance,
    Collection<ItemContext> itemContexts) {

    log.info("buildBffSearchInstance:: building instance {}", searchInstance::getId);
    return searchInstanceMapper.toBffSearchInstanceWithoutItems(searchInstance)
      .items(buildBffSearchItems(searchInstance, itemContexts));
  }

  private static List<BffSearchItem> buildBffSearchItems(SearchInstance searchInstance,
    Collection<ItemContext> itemContexts) {

    log.info("buildBffSearchItems:: building items for instance {}", searchInstance::getId);
    Set<String> itemIdsFromCurrentInstance = searchInstance.getItems()
      .stream()
      .map(SearchItem::getId)
      .collect(toSet());

    return itemContexts.stream()
      .filter(context -> itemIdsFromCurrentInstance.contains(context.item().getId()))
      .map(context -> buildBffSearchItem(context, searchInstance))
      .toList();
  }

  private static BffSearchItem buildBffSearchItem(ItemContext itemContext, SearchInstance searchInstance) {
    final Item item = itemContext.item();
    log.debug("buildBffSearchItem:: building search item {}", item::getId);

    BffSearchItem bffSearchItem = new BffSearchItem()
      .id(item.getId())
      .tenantId(itemContext.tenantId())
      .holdingsRecordId(toUUID(item.getHoldingsRecordId()))
      .instanceId(toUUID(searchInstance.getId()))
      .title(searchInstance.getTitle())
      .barcode(item.getBarcode())
      .enumeration(item.getEnumeration())
      .chronology(item.getChronology())
      .displaySummary(item.getDisplaySummary())
      .volume(item.getVolume())
      .inTransitDestinationServicePointId(toUUID(item.getInTransitDestinationServicePointId()));

    Optional.ofNullable(searchInstance.getContributors())
      .map(contributors -> contributors.stream()
        .map(Contributor::getName)
        .map(name -> new Contributor().name(name))
        .toList())
      .ifPresent(bffSearchItem::setContributors);

    Optional.ofNullable(item.getEffectiveCallNumberComponents())
      .map(itemCn -> new BffSearchItemCallNumberComponents()
        .callNumber(itemCn.getCallNumber())
        .prefix(itemCn.getPrefix())
        .suffix(itemCn.getSuffix()))
      .ifPresent(bffSearchItem::setCallNumberComponents);

    Optional.ofNullable(item.getEffectiveCallNumberComponents())
      .map(ItemEffectiveCallNumberComponents::getCallNumber)
      .ifPresent(bffSearchItem::setCallNumber);

    Optional.ofNullable(itemContext.servicePoint())
      .map(sp -> new BffSearchItemInTransitDestinationServicePoint()
        .id(toUUID(sp.getId()))
        .name(sp.getName()))
      .ifPresent(bffSearchItem::setInTransitDestinationServicePoint);

    Optional.ofNullable(itemContext.location())
      .map(loc -> new BffSearchItemLocation().name(loc.getName()))
      .ifPresent(bffSearchItem::setLocation);

    Optional.ofNullable(itemContext.materialType())
      .map(mt -> new BffSearchItemMaterialType().name(mt.getName()))
      .ifPresent(bffSearchItem::setMaterialType);

    Optional.ofNullable(item.getCopyNumber())
      .or(() -> Optional.ofNullable(itemContext.holdingsRecord().getCopyNumber()))
      .ifPresent(bffSearchItem::setCopyNumber);

    Optional.ofNullable(item.getStatus())
      .map(status -> new BffSearchItemStatus()
        .name(status.getName().getValue())
        .date(status.getDate()))
      .ifPresent(bffSearchItem::setStatus);

    return bffSearchItem;
  }

  private static UUID toUUID(String uuidString) {
    return Optional.ofNullable(uuidString)
      .map(UUID::fromString)
      .orElse(null);
  }

  private static <T> Collection<String> extractUniqueValues(Collection<T> objects,
    Function<T, String> valueExtractor) {

    return objects.stream()
      .map(valueExtractor)
      .filter(StringUtils::isNotBlank)
      .collect(toSet());
  }

  private record ItemContext(Item item, String tenantId, HoldingsRecord holdingsRecord,
    Location location, MaterialType materialType, ServicePoint servicePoint) { }

}
