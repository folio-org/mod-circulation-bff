package org.folio.circulationbff.service.impl;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.collections4.ListUtils;
import org.folio.circulationbff.client.HoldingsStorageClient;
import org.folio.circulationbff.client.InstanceStorageClient;
import org.folio.circulationbff.client.ItemStorageClient;
import org.folio.circulationbff.client.SearchItemsClient;
import org.folio.circulationbff.client.ServicePointClient;
import org.folio.circulationbff.domain.dto.BffSearchItemCallNumberComponents;
import org.folio.circulationbff.domain.dto.BffSearchItemInTransitDestinationServicePoint;
import org.folio.circulationbff.domain.dto.ConsortiumItem;
import org.folio.circulationbff.domain.dto.ConsortiumItemBatchRequest;
import org.folio.circulationbff.domain.dto.HoldingsRecord;
import org.folio.circulationbff.domain.dto.HoldingsRecords;
import org.folio.circulationbff.domain.dto.Instance;
import org.folio.circulationbff.domain.dto.Instances;
import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.ItemEffectiveCallNumberComponents;
import org.folio.circulationbff.domain.dto.Items;
import org.folio.circulationbff.domain.dto.LoanItem;
import org.folio.circulationbff.domain.dto.ServicePoint;
import org.folio.circulationbff.domain.dto.ServicePoints;
import org.folio.circulationbff.domain.model.LoanEnrichment;
import org.folio.circulationbff.service.BulkFetchingService;
import org.folio.circulationbff.service.LoanEnrichmentService;
import org.folio.circulationbff.service.TenantService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// SystemUserScopedExecutionService is deprecated for removal, but folio-spring-support 10.x has
// no replacement that runs as the system user. Calls here must keep using the system user.
@SuppressWarnings("removal")
@Log4j2
@Service
@RequiredArgsConstructor
public class LoanEnrichmentServiceImpl implements LoanEnrichmentService {

  static final int BATCH_CHUNK_SIZE = 100;
  private static final String ID_IDENTIFIER_TYPE = "id";

  private final TenantService tenantService;
  private final BulkFetchingService fetchingService;
  private final SearchItemsClient searchItemsClient;
  private final ItemStorageClient itemStorageClient;
  private final HoldingsStorageClient holdingsStorageClient;
  private final ServicePointClient servicePointClient;
  private final InstanceStorageClient instanceStorageClient;
  private final SystemUserScopedExecutionService executionService;

  @Override
  public Map<String, LoanEnrichment> enrich(Collection<LoanItem> loanItems) {
    Map<String, String> instanceIdByItemId = new LinkedHashMap<>();
    for (LoanItem loanItem : emptyIfNull(loanItems)) {
      if (loanItem != null && isNotBlank(loanItem.getId())) {
        instanceIdByItemId.putIfAbsent(loanItem.getId(), loanItem.getInstanceId());
      }
    }

    if (instanceIdByItemId.isEmpty()) {
      log.info("enrich:: no items to enrich");
      return Map.of();
    }

    String centralTenantId = tenantService.getCentralTenantId().orElse(null);
    if (centralTenantId == null) {
      log.warn("enrich:: central tenant ID is unknown, {} item(s) will not be enriched",
        instanceIdByItemId.size());
      return Map.of();
    }

    Map<String, LoanEnrichment> result = new HashMap<>();
    for (List<String> chunk : ListUtils.partition(new ArrayList<>(instanceIdByItemId.keySet()),
      BATCH_CHUNK_SIZE)) {

      result.putAll(enrichChunk(chunk, instanceIdByItemId, centralTenantId));
    }

    log.info("enrich:: requested={}, enriched={}", instanceIdByItemId.size(), result.size());
    return result;
  }

  private Map<String, LoanEnrichment> enrichChunk(List<String> itemIds,
    Map<String, String> instanceIdByItemId, String centralTenantId) {

    Map<String, ConsortiumItem> found = findConsortiumItems(itemIds, centralTenantId);
    log.info("enrichChunk:: requested={}, found={}, missing={}", itemIds.size(), found.size(),
      itemIds.size() - found.size());

    Map<String, List<ConsortiumItem>> byTenant = found.values().stream()
      .collect(groupingBy(ConsortiumItem::getTenantId));

    Map<String, LoanEnrichment> result = new HashMap<>();
    byTenant.forEach((tenantId, consortiumItems) -> result.putAll(
      enrichInTenant(tenantId, consortiumItems, instanceIdByItemId, centralTenantId)));

    return result;
  }

  private Map<String, ConsortiumItem> findConsortiumItems(List<String> itemIds,
    String centralTenantId) {

    var request = new ConsortiumItemBatchRequest()
      .identifierType(ID_IDENTIFIER_TYPE)
      .identifierValues(itemIds);

    var response = executionService.executeSystemUserScoped(centralTenantId,
      () -> searchItemsClient.searchItemsBatch(request));

    Map<String, ConsortiumItem> byId = new LinkedHashMap<>();
    for (ConsortiumItem consortiumItem : emptyIfNull(
      response == null ? null : response.getItems())) {

      if (isBlank(consortiumItem.getId()) || isBlank(consortiumItem.getTenantId())) {
        log.warn("findConsortiumItems:: skipping search result without id or tenantId");
        continue;
      }
      byId.putIfAbsent(consortiumItem.getId(), consortiumItem);
    }
    return byId;
  }

  private Map<String, LoanEnrichment> enrichInTenant(String tenantId,
    List<ConsortiumItem> consortiumItems, Map<String, String> instanceIdByItemId,
    String centralTenantId) {

    return executionService.executeSystemUserScoped(tenantId,
      () -> doEnrichInTenant(tenantId, consortiumItems, instanceIdByItemId, centralTenantId));
  }

  private Map<String, LoanEnrichment> doEnrichInTenant(String tenantId,
    List<ConsortiumItem> consortiumItems, Map<String, String> instanceIdByItemId,
    String centralTenantId) {

    Set<String> itemIds = consortiumItems.stream()
      .map(ConsortiumItem::getId)
      .collect(toCollection(LinkedHashSet::new));

    Map<String, Item> items = fetchingService.fetchByIds(itemStorageClient, itemIds,
      Items::getItems, Item::getId);

    Map<String, HoldingsRecord> holdings = fetchHoldings(items.values());
    Map<String, ServicePoint> servicePoints = fetchServicePoints(items.values());

    Map<String, String> instanceIdByItem = new LinkedHashMap<>();
    for (ConsortiumItem consortiumItem : consortiumItems) {
      String loanInstanceId = instanceIdByItemId.get(consortiumItem.getId());
      instanceIdByItem.put(consortiumItem.getId(),
        isNotBlank(loanInstanceId) ? loanInstanceId : consortiumItem.getInstanceId());
    }
    Map<String, Instance> instances = fetchInstances(tenantId, centralTenantId,
      instanceIdByItem.values());

    Map<String, LoanEnrichment> result = new HashMap<>();
    int missingItems = 0;
    int missingInstances = 0;
    for (ConsortiumItem consortiumItem : consortiumItems) {
      Item item = items.get(consortiumItem.getId());
      if (item == null) {
        missingItems++;
        continue;
      }
      Instance instance = instances.get(instanceIdByItem.get(consortiumItem.getId()));
      if (instance == null) {
        missingInstances++;
        continue;
      }
      result.put(consortiumItem.getId(),
        buildEnrichment(tenantId, item, instance, holdings, servicePoints));
    }

    if (missingItems > 0 || missingInstances > 0) {
      log.warn("doEnrichInTenant:: tenant={}, itemsNotFound={}, instancesNotFound={}",
        tenantId, missingItems, missingInstances);
    }
    return result;
  }

  private Map<String, HoldingsRecord> fetchHoldings(Collection<Item> items) {
    Set<String> ids = items.stream()
      .filter(item -> isBlank(item.getCopyNumber()))
      .map(Item::getHoldingsRecordId)
      .filter(id -> isNotBlank(id))
      .collect(toCollection(LinkedHashSet::new));

    if (ids.isEmpty()) {
      return Map.of();
    }
    return fetchingService.fetchByIds(holdingsStorageClient, ids,
      HoldingsRecords::getHoldingsRecords, HoldingsRecord::getId);
  }

  private Map<String, ServicePoint> fetchServicePoints(Collection<Item> items) {
    Set<String> ids = items.stream()
      .map(Item::getInTransitDestinationServicePointId)
      .filter(id -> isNotBlank(id))
      .collect(toCollection(LinkedHashSet::new));

    if (ids.isEmpty()) {
      return Map.of();
    }
    return fetchingService.fetchByIds(servicePointClient, ids,
      ServicePoints::getServicepoints, ServicePoint::getId);
  }

  private Map<String, Instance> fetchInstances(String tenantId, String centralTenantId,
    Collection<String> instanceIds) {

    Set<String> ids = instanceIds.stream()
      .filter(id -> isNotBlank(id))
      .collect(toCollection(LinkedHashSet::new));

    if (ids.isEmpty()) {
      return Map.of();
    }

    Map<String, Instance> instances = new HashMap<>(fetchingService.fetchByIds(
      instanceStorageClient, ids, Instances::getInstances, Instance::getId));

    Set<String> missing = new LinkedHashSet<>(ids);
    missing.removeAll(instances.keySet());
    if (!missing.isEmpty() && !Objects.equals(tenantId, centralTenantId)) {
      log.info("fetchInstances:: {} instance(s) not found in tenant {}, retrying in central",
        missing.size(), tenantId);
      instances.putAll(executionService.executeSystemUserScoped(centralTenantId,
        () -> fetchingService.fetchByIds(instanceStorageClient, missing,
          Instances::getInstances, Instance::getId)));
    }
    return instances;
  }

  private static LoanEnrichment buildEnrichment(String tenantId, Item item, Instance instance,
    Map<String, HoldingsRecord> holdings, Map<String, ServicePoint> servicePoints) {

    var callNumberComponents = item.getEffectiveCallNumberComponents();
    var servicePoint = lookup(servicePoints, item.getInTransitDestinationServicePointId());

    return LoanEnrichment.builder()
      .tenantId(tenantId)
      .callNumber(ofNullable(callNumberComponents)
        .map(ItemEffectiveCallNumberComponents::getCallNumber)
        .orElse(null))
      .callNumberComponents(ofNullable(callNumberComponents)
        .map(c -> new BffSearchItemCallNumberComponents()
          .callNumber(c.getCallNumber())
          .prefix(c.getPrefix())
          .suffix(c.getSuffix()))
        .orElse(null))
      .volume(item.getVolume())
      .copyNumber(resolveCopyNumber(item, holdings))
      .chronology(item.getChronology())
      .enumeration(item.getEnumeration())
      .displaySummary(item.getDisplaySummary())
      .accessionNumber(item.getAccessionNumber())
      .inTransitDestinationServicePoint(ofNullable(servicePoint)
        .map(sp -> new BffSearchItemInTransitDestinationServicePoint()
          .id(UUID.fromString(sp.getId()))
          .name(sp.getName()))
        .orElse(null))
      .editions(instance.getEditions() == null ? new LinkedHashSet<>()
        : new LinkedHashSet<>(instance.getEditions()))
      .physicalDescriptions(emptyListIfNull(instance.getPhysicalDescriptions()))
      .contributors(emptyListIfNull(instance.getContributors()))
      .publication(emptyListIfNull(instance.getPublication()))
      .build();
  }

  private static String resolveCopyNumber(Item item, Map<String, HoldingsRecord> holdings) {
    if (isNotBlank(item.getCopyNumber())) {
      return item.getCopyNumber();
    }
    return ofNullable(lookup(holdings, item.getHoldingsRecordId()))
      .map(HoldingsRecord::getCopyNumber)
      .orElse(null);
  }

  /** Null-safe lookup: immutable maps such as {@code Map.of()} throw NPE on a null key. */
  private static <V> V lookup(Map<String, V> map, String key) {
    return key == null ? null : map.get(key);
  }

  private static <T> List<T> emptyListIfNull(List<T> list) {
    return list == null ? new ArrayList<>() : new ArrayList<>(list);
  }
}
