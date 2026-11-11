package org.folio.circulationbff.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.folio.circulationbff.client.GetByQueryParamsClient;
import org.folio.circulationbff.client.HoldingsStorageClient;
import org.folio.circulationbff.client.InstanceStorageClient;
import org.folio.circulationbff.client.ItemStorageClient;
import org.folio.circulationbff.client.SearchItemsClient;
import org.folio.circulationbff.client.ServicePointClient;
import org.folio.circulationbff.domain.dto.BffSearchItemCallNumberComponents;
import org.folio.circulationbff.domain.dto.ConsortiumItem;
import org.folio.circulationbff.domain.dto.ConsortiumItemBatchRequest;
import org.folio.circulationbff.domain.dto.ConsortiumItemCollection;
import org.folio.circulationbff.domain.dto.HoldingsRecord;
import org.folio.circulationbff.domain.dto.Instance;
import org.folio.circulationbff.domain.dto.InstanceContributorsInner;
import org.folio.circulationbff.domain.dto.InstancePublicationInner;
import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.ItemEffectiveCallNumberComponents;
import org.folio.circulationbff.domain.dto.LoanItem;
import org.folio.circulationbff.domain.dto.ServicePoint;
import org.folio.circulationbff.domain.model.LoanEnrichment;
import org.folio.circulationbff.service.impl.LoanEnrichmentServiceImpl;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.scope.FolioExecutionContextService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanEnrichmentServiceTest {

  private static final String CENTRAL = "consortium";
  private static final String COLLEGE = "college";
  private static final String UNIVERSITY = "university";

  private static final String ITEM_1 = "item-1";
  private static final String ITEM_2 = "item-2";
  private static final String ITEM_3 = "item-3";
  private static final String DCB_INSTANCE_ID = "9d1b77e4-f02e-4b7f-b296-3f2042ddac54";
  private static final String INSTANCE_1 = "instance-1";
  private static final String INSTANCE_2 = "instance-2";
  private static final String SERVICE_POINT_ID = UUID.randomUUID().toString();

  @Mock private TenantService tenantService;
  @Mock private BulkFetchingService fetchingService;
  @Mock private SearchItemsClient searchItemsClient;
  @Mock private ItemStorageClient itemStorageClient;
  @Mock private HoldingsStorageClient holdingsStorageClient;
  @Mock private ServicePointClient servicePointClient;
  @Mock private InstanceStorageClient instanceStorageClient;
  @Mock private FolioExecutionContextService executionService;
  @Mock private FolioExecutionContext folioExecutionContext;

  @InjectMocks private LoanEnrichmentServiceImpl service;

  // Each test calls the stubXxx helpers it needs. They are not in @BeforeEach on purpose:
  // with strict stubs, tests that return early would fail with UnnecessaryStubbingException.

  @Test
  void enrichReturnsEmptyMapForEmptyInput() {
    var result = service.enrich(List.of());

    assertTrue(result.isEmpty());
    verifyNoInteractions(searchItemsClient, executionService, fetchingService, tenantService);
  }

  @Test
  void enrichReturnsEmptyMapWhenCentralTenantUnknown() {
    when(tenantService.getCentralTenantId()).thenReturn(Optional.empty());

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1)));

    assertTrue(result.isEmpty());
    verifyNoInteractions(searchItemsClient, executionService, fetchingService);
  }

  @Test
  void enrichSingleItem() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, "batch-instance")), ITEM_1);
    var storageItem = storageItem(ITEM_1).copyNumber("cn")
      .inTransitDestinationServicePointId(SERVICE_POINT_ID);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of(ITEM_1, storageItem));
    stubFetch(servicePointClient, Set.of(SERVICE_POINT_ID),
      Map.of(SERVICE_POINT_ID, new ServicePoint().id(SERVICE_POINT_ID).name("Circ desk")));
    var contributor = new InstanceContributorsInner().name("Author").primary(true);
    var publication = new InstancePublicationInner().dateOfPublication("2001");
    var instance = storageInstance(INSTANCE_1)
      .physicalDescriptions(List.of("300 pages"))
      .contributors(List.of(contributor))
      .publication(List.of(publication));
    stubFetch(instanceStorageClient, Set.of(INSTANCE_1), Map.of(INSTANCE_1, instance));

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1)));

    assertEquals(1, result.size());
    LoanEnrichment enrichment = result.get(ITEM_1);
    assertNotNull(enrichment);
    assertEquals(COLLEGE, enrichment.tenantId());
    assertEquals("CN-item-1", enrichment.callNumber());
    assertEquals(new BffSearchItemCallNumberComponents()
      .callNumber("CN-item-1").prefix("P").suffix("S"), enrichment.callNumberComponents());
    assertEquals("v", enrichment.volume());
    assertEquals("cn", enrichment.copyNumber());
    assertEquals("c", enrichment.chronology());
    assertEquals("e", enrichment.enumeration());
    assertEquals("d", enrichment.displaySummary());
    assertEquals("a", enrichment.accessionNumber());
    assertEquals(UUID.fromString(SERVICE_POINT_ID),
      enrichment.inTransitDestinationServicePoint().getId());
    assertEquals("Circ desk", enrichment.inTransitDestinationServicePoint().getName());
    assertEquals(Set.of("ed"), enrichment.editions());
    assertEquals(List.of("300 pages"), enrichment.physicalDescriptions());
    assertEquals(List.of(contributor), enrichment.contributors());
    assertEquals(List.of(publication), enrichment.publication());

    verify(executionService, times(1)).execute(eq(CENTRAL),
      any(FolioExecutionContext.class), any(Callable.class));
    verify(executionService, times(1)).execute(eq(COLLEGE),
      any(FolioExecutionContext.class), any(Callable.class));
    var captor = ArgumentCaptor.forClass(ConsortiumItemBatchRequest.class);
    verify(searchItemsClient).searchItemsBatch(captor.capture());
    assertEquals("id", captor.getValue().getIdentifierType());
    assertEquals(List.of(ITEM_1), captor.getValue().getIdentifierValues());
  }

  @Test
  void enrichDeduplicatesInputIds() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(new ConsortiumItemCollection(), ITEM_1);

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1), loanItem(ITEM_1, INSTANCE_1),
      loanItem(ITEM_1, INSTANCE_1)));

    assertTrue(result.isEmpty());
    var captor = ArgumentCaptor.forClass(ConsortiumItemBatchRequest.class);
    verify(searchItemsClient, times(1)).searchItemsBatch(captor.capture());
    assertEquals(List.of(ITEM_1), captor.getValue().getIdentifierValues());
  }

  @Test
  void enrichIgnoresLoanItemsWithoutId() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(new ConsortiumItemCollection(), ITEM_1);

    var result = service.enrich(List.of(loanItem(null, INSTANCE_1), loanItem(ITEM_1, INSTANCE_1)));

    assertTrue(result.isEmpty());
    var captor = ArgumentCaptor.forClass(ConsortiumItemBatchRequest.class);
    verify(searchItemsClient).searchItemsBatch(captor.capture());
    assertEquals(List.of(ITEM_1), captor.getValue().getIdentifierValues());
  }

  @Test
  void enrichSplitsIntoChunks() {
    stubCentralTenant();
    stubScopedExecution();
    when(searchItemsClient.searchItemsBatch(any(ConsortiumItemBatchRequest.class)))
      .thenReturn(new ConsortiumItemCollection());
    var loanItems = IntStream.range(0, 250)
      .mapToObj(i -> loanItem("item-" + i, "instance-" + i))
      .toList();

    var result = service.enrich(loanItems);

    assertTrue(result.isEmpty());
    var captor = ArgumentCaptor.forClass(ConsortiumItemBatchRequest.class);
    verify(searchItemsClient, times(3)).searchItemsBatch(captor.capture());
    assertEquals(List.of(100, 100, 50), captor.getAllValues().stream()
      .map(request -> request.getIdentifierValues().size())
      .toList());
  }

  @Test
  void enrichGroupsByTenant() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, INSTANCE_1),
      consortiumItem(ITEM_2, UNIVERSITY, INSTANCE_2)), ITEM_1, ITEM_2);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of(ITEM_1, storageItemWithCopy(ITEM_1)));
    stubFetch(itemStorageClient, Set.of(ITEM_2), Map.of(ITEM_2, storageItemWithCopy(ITEM_2)));
    stubFetch(instanceStorageClient, Set.of(INSTANCE_1),
      Map.of(INSTANCE_1, storageInstance(INSTANCE_1)));
    stubFetch(instanceStorageClient, Set.of(INSTANCE_2),
      Map.of(INSTANCE_2, storageInstance(INSTANCE_2)));

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1), loanItem(ITEM_2, INSTANCE_2)));

    assertEquals(2, result.size());
    assertEquals(COLLEGE, result.get(ITEM_1).tenantId());
    assertEquals(UNIVERSITY, result.get(ITEM_2).tenantId());
    verify(executionService, times(1)).execute(eq(COLLEGE),
      any(FolioExecutionContext.class), any(Callable.class));
    verify(executionService, times(1)).execute(eq(UNIVERSITY),
      any(FolioExecutionContext.class), any(Callable.class));
  }

  @Test
  void enrichSkipsItemMissingFromBatchResponse() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, INSTANCE_1)), ITEM_1, ITEM_2);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of(ITEM_1, storageItemWithCopy(ITEM_1)));
    stubFetch(instanceStorageClient, Set.of(INSTANCE_1),
      Map.of(INSTANCE_1, storageInstance(INSTANCE_1)));

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1), loanItem(ITEM_2, INSTANCE_2)));

    assertEquals(Set.of(ITEM_1), result.keySet());
  }

  @Test
  void enrichUsesRealSearchInstanceWhenLoanCarriesDcbInstanceId() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, INSTANCE_1)), ITEM_1);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of(ITEM_1, storageItemWithCopy(ITEM_1)));
    stubFetch(instanceStorageClient, Set.of(INSTANCE_1),
      Map.of(INSTANCE_1, storageInstance(INSTANCE_1)));

    var result = service.enrich(List.of(loanItem(ITEM_1, DCB_INSTANCE_ID)));

    assertEquals(Set.of(ITEM_1), result.keySet());
    assertEquals(Set.of("ed"), result.get(ITEM_1).editions());
  }

  @Test
  void enrichUsesRealLoanInstanceWhenSearchResultCarriesDcbInstanceId() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, DCB_INSTANCE_ID)), ITEM_1);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of(ITEM_1, storageItemWithCopy(ITEM_1)));
    stubFetch(instanceStorageClient, Set.of(INSTANCE_1),
      Map.of(INSTANCE_1, storageInstance(INSTANCE_1)));

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1)));

    assertEquals(Set.of(ITEM_1), result.keySet());
    assertEquals(Set.of("ed"), result.get(ITEM_1).editions());
  }

  @Test
  void enrichDoesNotFetchInstanceWhenBothSourcesCarryDcbInstanceId() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, DCB_INSTANCE_ID)), ITEM_1);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of(ITEM_1, storageItemWithCopy(ITEM_1)));

    var result = service.enrich(List.of(loanItem(ITEM_1, DCB_INSTANCE_ID)));

    assertEquals(Set.of(ITEM_1), result.keySet());
    assertDcbEnrichment(result.get(ITEM_1));
    verify(fetchingService, never()).fetchByIds(eq(instanceStorageClient), anyCollection(),
      any(), any());
  }

  @Test
  void enrichDoesNotFetchDcbInstanceFromSearchResultFallback() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, DCB_INSTANCE_ID)), ITEM_1);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of(ITEM_1, storageItemWithCopy(ITEM_1)));

    var result = service.enrich(List.of(loanItem(ITEM_1, null)));

    assertEquals(Set.of(ITEM_1), result.keySet());
    assertDcbEnrichment(result.get(ITEM_1));
    verify(fetchingService, never()).fetchByIds(eq(instanceStorageClient), anyCollection(),
      any(), any());
  }

  @Test
  void enrichFetchesInstanceOnlyForNonDcbItemsInMixedBatch() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, INSTANCE_1),
      consortiumItem(ITEM_2, COLLEGE, DCB_INSTANCE_ID)), ITEM_1, ITEM_2);
    stubFetch(itemStorageClient, Set.of(ITEM_1, ITEM_2),
      Map.of(ITEM_1, storageItemWithCopy(ITEM_1), ITEM_2, storageItemWithCopy(ITEM_2)));
    stubFetch(instanceStorageClient, Set.of(INSTANCE_1),
      Map.of(INSTANCE_1, storageInstance(INSTANCE_1)));

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1),
      loanItem(ITEM_2, DCB_INSTANCE_ID)));

    assertEquals(Set.of(ITEM_1, ITEM_2), result.keySet());
    assertDcbEnrichment(result.get(ITEM_2));
  }

  @Test
  void enrichDeduplicatesBatchResponse() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, INSTANCE_1),
      consortiumItem(ITEM_1, COLLEGE, INSTANCE_1)), ITEM_1);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of(ITEM_1, storageItemWithCopy(ITEM_1)));
    stubFetch(instanceStorageClient, Set.of(INSTANCE_1),
      Map.of(INSTANCE_1, storageInstance(INSTANCE_1)));

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1)));

    assertEquals(Set.of(ITEM_1), result.keySet());
    verify(executionService, times(1)).execute(eq(COLLEGE),
      any(FolioExecutionContext.class), any(Callable.class));
  }

  @Test
  void enrichSkipsConsortiumItemWithoutTenant() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, null, INSTANCE_1)), ITEM_1);

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1)));

    assertTrue(result.isEmpty());
    verify(executionService, times(1)).execute(any(String.class), any(FolioExecutionContext.class),
      any(Callable.class));
    verifyNoInteractions(fetchingService);
  }

  @Test
  void enrichSkipsItemMissingFromItemStorage() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, INSTANCE_1)), ITEM_1);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of());
    stubFetch(instanceStorageClient, Set.of(INSTANCE_1),
      Map.of(INSTANCE_1, storageInstance(INSTANCE_1)));

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1)));

    assertTrue(result.isEmpty());
  }

  @Test
  void enrichFallsBackToHoldingsCopyNumber() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, INSTANCE_1)), ITEM_1);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of(ITEM_1, storageItem(ITEM_1)));
    stubFetch(holdingsStorageClient, Set.of("h-item-1"),
      Map.of("h-item-1", new HoldingsRecord().id("h-item-1").copyNumber("holdings-copy")));
    stubFetch(instanceStorageClient, Set.of(INSTANCE_1),
      Map.of(INSTANCE_1, storageInstance(INSTANCE_1)));

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1)));

    assertEquals("holdings-copy", result.get(ITEM_1).copyNumber());
  }

  @Test
  void enrichKeepsItemCopyNumberAndSkipsHoldingsFetch() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, INSTANCE_1)), ITEM_1);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of(ITEM_1, storageItemWithCopy(ITEM_1)));
    stubFetch(instanceStorageClient, Set.of(INSTANCE_1),
      Map.of(INSTANCE_1, storageInstance(INSTANCE_1)));

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1)));

    assertEquals("cn", result.get(ITEM_1).copyNumber());
    verify(fetchingService, never()).fetchByIds(eq(holdingsStorageClient), anyCollection(),
      any(), any());
  }

  @Test
  void enrichToleratesMissingHoldingsRecord() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, INSTANCE_1)), ITEM_1);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of(ITEM_1, storageItem(ITEM_1)));
    stubFetch(holdingsStorageClient, Set.of("h-item-1"), Map.of());
    stubFetch(instanceStorageClient, Set.of(INSTANCE_1),
      Map.of(INSTANCE_1, storageInstance(INSTANCE_1)));

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1)));

    assertNull(result.get(ITEM_1).copyNumber());
  }

  @Test
  void enrichWithoutServicePointDoesNotFetchServicePoints() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, INSTANCE_1)), ITEM_1);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of(ITEM_1, storageItemWithCopy(ITEM_1)));
    stubFetch(instanceStorageClient, Set.of(INSTANCE_1),
      Map.of(INSTANCE_1, storageInstance(INSTANCE_1)));

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1)));

    assertNull(result.get(ITEM_1).inTransitDestinationServicePoint());
    verify(fetchingService, never()).fetchByIds(eq(servicePointClient), anyCollection(),
      any(), any());
  }

  @Test
  void enrichToleratesMissingServicePointRecord() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, INSTANCE_1)), ITEM_1);
    var storageItem = storageItemWithCopy(ITEM_1)
      .inTransitDestinationServicePointId(SERVICE_POINT_ID);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of(ITEM_1, storageItem));
    stubFetch(servicePointClient, Set.of(SERVICE_POINT_ID), Map.of());
    stubFetch(instanceStorageClient, Set.of(INSTANCE_1),
      Map.of(INSTANCE_1, storageInstance(INSTANCE_1)));

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1)));

    assertEquals(Set.of(ITEM_1), result.keySet());
    assertNull(result.get(ITEM_1).inTransitDestinationServicePoint());
  }

  @Test
  void enrichPrefersLoanItemInstanceId() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, "batch-inst")), ITEM_1);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of(ITEM_1, storageItemWithCopy(ITEM_1)));
    stubFetch(instanceStorageClient, Set.of("loan-inst"),
      Map.of("loan-inst", storageInstance("loan-inst")));

    var result = service.enrich(List.of(loanItem(ITEM_1, "loan-inst")));

    assertEquals(Set.of(ITEM_1), result.keySet());
  }

  @Test
  void enrichUsesBatchInstanceIdWhenLoanItemHasNone() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, "batch-inst")), ITEM_1);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of(ITEM_1, storageItemWithCopy(ITEM_1)));
    stubFetch(instanceStorageClient, Set.of("batch-inst"),
      Map.of("batch-inst", storageInstance("batch-inst")));

    var result = service.enrich(List.of(loanItem(ITEM_1, null)));

    assertEquals(Set.of(ITEM_1), result.keySet());
  }

  @Test
  void enrichRetriesMissingInstanceInCentralTenant() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, INSTANCE_1)), ITEM_1);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of(ITEM_1, storageItemWithCopy(ITEM_1)));
    stubFetch(instanceStorageClient, Set.of(INSTANCE_1), Map.of(),
      Map.of(INSTANCE_1, storageInstance(INSTANCE_1)));

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1)));

    assertEquals(Set.of(ITEM_1), result.keySet());
    // central: one call for the batch search and one for the instance retry
    verify(executionService, times(2)).execute(eq(CENTRAL),
      any(FolioExecutionContext.class), any(Callable.class));
    verify(fetchingService, times(2)).fetchByIds(eq(instanceStorageClient), anyCollection(),
      any(), any());
  }

  @Test
  void enrichDoesNotRetryInCentralWhenItemTenantIsCentral() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, CENTRAL, INSTANCE_1)), ITEM_1);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of(ITEM_1, storageItemWithCopy(ITEM_1)));
    stubFetch(instanceStorageClient, Set.of(INSTANCE_1), Map.of());

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1)));

    assertTrue(result.isEmpty());
    verify(fetchingService, times(1)).fetchByIds(eq(instanceStorageClient), anyCollection(),
      any(), any());
  }

  @Test
  void enrichSkipsItemWhenInstanceNotFoundAnywhere() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, INSTANCE_1)), ITEM_1);
    stubFetch(itemStorageClient, Set.of(ITEM_1), Map.of(ITEM_1, storageItemWithCopy(ITEM_1)));
    stubFetch(instanceStorageClient, Set.of(INSTANCE_1), Map.of(), Map.of());

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1)));

    assertTrue(result.isEmpty());
  }

  @Test
  void enrichFetchesEachInstanceOnce() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, INSTANCE_1),
      consortiumItem(ITEM_2, COLLEGE, INSTANCE_1),
      consortiumItem(ITEM_3, COLLEGE, INSTANCE_1)), ITEM_1, ITEM_2, ITEM_3);
    stubFetch(itemStorageClient, Set.of(ITEM_1, ITEM_2, ITEM_3), Map.of(
      ITEM_1, storageItemWithCopy(ITEM_1),
      ITEM_2, storageItemWithCopy(ITEM_2),
      ITEM_3, storageItemWithCopy(ITEM_3)));
    stubFetch(instanceStorageClient, Set.of(INSTANCE_1),
      Map.of(INSTANCE_1, storageInstance(INSTANCE_1)));

    var result = service.enrich(List.of(loanItem(ITEM_1, INSTANCE_1),
      loanItem(ITEM_2, INSTANCE_1), loanItem(ITEM_3, INSTANCE_1)));

    assertEquals(Set.of(ITEM_1, ITEM_2, ITEM_3), result.keySet());
  }

  @Test
  void enrichPropagatesFailureFromBatchCall() {
    stubCentralTenant();
    stubScopedExecution();
    when(searchItemsClient.searchItemsBatch(any(ConsortiumItemBatchRequest.class)))
      .thenThrow(new IllegalStateException("search is down"));
    var loanItems = List.of(loanItem(ITEM_1, INSTANCE_1));

    var exception = assertThrows(IllegalStateException.class, () -> service.enrich(loanItems));

    assertEquals("search is down", exception.getMessage());
  }

  @Test
  @SuppressWarnings({ "unchecked", "rawtypes" })
  void enrichPropagatesFailureFromStorage() {
    stubCentralTenant();
    stubScopedExecution();
    stubBatch(collection(consortiumItem(ITEM_1, COLLEGE, INSTANCE_1)), ITEM_1);
    when(fetchingService.fetchByIds((GetByQueryParamsClient) eq(itemStorageClient),
      anyCollection(), any(Function.class), any(Function.class)))
      .thenThrow(new IllegalStateException("storage is down"));
    var loanItems = List.of(loanItem(ITEM_1, INSTANCE_1));

    var exception = assertThrows(IllegalStateException.class, () -> service.enrich(loanItems));

    assertEquals("storage is down", exception.getMessage());
  }

  @Test
  void enrichHasNoReferenceDataDependencies() {
    var fieldTypes = Arrays.stream(LoanEnrichmentServiceImpl.class.getDeclaredFields())
      .map(field -> field.getType().getSimpleName())
      .toList();

    assertFalse(fieldTypes.stream().anyMatch(name -> name.contains("Location")
      || name.contains("MaterialType") || name.contains("LoanType")));
  }

  @Test
  void enrichWorkAmountIsProportionalToLoanedItems() {
    stubCentralTenant();
    stubScopedExecution();
    var itemIds = IntStream.range(0, 49).mapToObj(i -> "item-" + i).toList();
    var instanceIds = IntStream.range(0, 49).mapToObj(i -> "instance-" + i).toList();
    var loanItems = new ArrayList<LoanItem>();
    var consortiumItems = new ArrayList<ConsortiumItem>();
    var storageItems = new java.util.HashMap<String, Item>();
    var instances = new java.util.HashMap<String, Instance>();
    for (int i = 0; i < 49; i++) {
      loanItems.add(loanItem(itemIds.get(i), instanceIds.get(i)));
      consortiumItems.add(consortiumItem(itemIds.get(i), COLLEGE, instanceIds.get(i)));
      storageItems.put(itemIds.get(i), storageItemWithCopy(itemIds.get(i)));
      instances.put(instanceIds.get(i), storageInstance(instanceIds.get(i)));
    }
    stubBatch(collection(consortiumItems.toArray(ConsortiumItem[]::new)),
      itemIds.toArray(String[]::new));
    // the matcher requires exactly these 49 ids, nothing more
    stubFetch(itemStorageClient, new LinkedHashSet<>(itemIds), storageItems);
    stubFetch(instanceStorageClient, new LinkedHashSet<>(instanceIds), instances);

    var result = service.enrich(loanItems);

    assertEquals(49, result.size());
    verify(searchItemsClient, times(1)).searchItemsBatch(any(ConsortiumItemBatchRequest.class));
    verify(fetchingService, times(1)).fetchByIds(eq(itemStorageClient), anyCollection(), any(),
      any());
    verify(fetchingService, times(1)).fetchByIds(eq(instanceStorageClient), anyCollection(), any(),
      any());
    verify(fetchingService, never()).fetchByIds(eq(holdingsStorageClient), anyCollection(), any(),
      any());
  }

  // ---- helpers -----------------------------------------------------------------------------

  private static void assertDcbEnrichment(LoanEnrichment enrichment) {
    assertEquals(COLLEGE, enrichment.tenantId());
    assertTrue(enrichment.contributors().isEmpty());
    assertTrue(enrichment.publication().isEmpty());
    assertTrue(enrichment.editions().isEmpty());
    assertTrue(enrichment.physicalDescriptions().isEmpty());
  }

  private void stubCentralTenant() {
    when(tenantService.getCentralTenantId()).thenReturn(Optional.of(CENTRAL));
  }

  @SuppressWarnings("unchecked")
  private void stubScopedExecution() {
    when(executionService.execute(any(String.class), any(FolioExecutionContext.class),
      any(Callable.class)))
      .thenAnswer(invocation -> invocation.getArgument(2, Callable.class).call());
  }

  private void stubBatch(ConsortiumItemCollection response, String... expectedIds) {
    when(searchItemsClient.searchItemsBatch(argThat(request -> request != null
      && "id".equals(request.getIdentifierType())
      && request.getIdentifierValues().size() == expectedIds.length
      && Set.copyOf(request.getIdentifierValues()).equals(Set.of(expectedIds)))))
      .thenReturn(response);
  }

  /**
   * Stubs a bulk fetch for the client. The matcher accepts exactly the expected ids, and rejects
   * duplicates and extra ids. Several results are returned for consecutive calls.
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void stubFetch(GetByQueryParamsClient client, Set<String> expectedIds, Map first,
    Map... others) {

    when(fetchingService.fetchByIds(eq(client),
      argThat(ids -> ids != null && ids.size() == expectedIds.size()
        && Set.copyOf(ids).equals(expectedIds)),
      any(Function.class), any(Function.class)))
      .thenReturn(first, others);
  }

  private static LoanItem loanItem(String itemId, String instanceId) {
    return new LoanItem().id(itemId).instanceId(instanceId);
  }

  private static ConsortiumItem consortiumItem(String itemId, String tenantId,
    String instanceId) {

    return new ConsortiumItem().id(itemId).tenantId(tenantId).instanceId(instanceId)
      .holdingsRecordId("h-" + itemId);
  }

  private static ConsortiumItemCollection collection(ConsortiumItem... items) {
    return new ConsortiumItemCollection().items(List.of(items)).totalRecords(items.length);
  }

  private static Item storageItem(String itemId) {
    return new Item().id(itemId).holdingsRecordId("h-" + itemId)
      .effectiveCallNumberComponents(new ItemEffectiveCallNumberComponents()
        .callNumber("CN-" + itemId).prefix("P").suffix("S"))
      .volume("v").chronology("c").enumeration("e").displaySummary("d").accessionNumber("a");
  }

  private static Item storageItemWithCopy(String itemId) {
    return storageItem(itemId).copyNumber("cn");
  }

  private static Instance storageInstance(String instanceId) {
    return new Instance().id(instanceId).editions(new LinkedHashSet<>(List.of("ed")));
  }
}
