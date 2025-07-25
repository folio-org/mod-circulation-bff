package org.folio.circulationbff.service;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.folio.circulationbff.client.feign.SearchInstancesClient;
import org.folio.circulationbff.domain.dto.BffSearchInstance;
import org.folio.circulationbff.domain.dto.Instance;
import org.folio.circulationbff.domain.dto.SearchInstance;
import org.folio.circulationbff.domain.dto.SearchInstances;
import org.folio.circulationbff.domain.dto.SearchItem;
import org.folio.circulationbff.domain.mapping.SearchInstanceMapper;
import org.folio.circulationbff.service.impl.SearchServiceImpl;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

  private static final String TENANT_ID_CONSORTIUM = "consortium";

  @Mock private SearchInstancesClient searchInstancesClient;
  @Mock private SearchInstanceMapper searchInstanceMapper;
  @Mock private SystemUserScopedExecutionService executionService;
  @Mock private BulkFetchingService fetchingService;

  @InjectMocks
  private SearchServiceImpl searchService;

  @Test
  void searchInstanceByItemId() {
    String itemId = UUID.randomUUID().toString();
    SearchInstance instance = new SearchInstance();

    SearchInstances mockSearchResponse = new SearchInstances()
      .instances(List.of(instance))
      .totalRecords(1);
    String query = "items.id==" + itemId;
    when(searchInstancesClient.findInstances(query, true))
      .thenReturn(mockSearchResponse);

    SearchInstance response = searchService.findInstanceByItemId(itemId);
    assertEquals(response, instance);
  }

  @Test
  void searchFindsNoInstances() {
    String instanceId = UUID.randomUUID().toString();
    String query = "id==" + instanceId;

    SearchInstances mockSearchResponse = new SearchInstances()
      .instances(emptyList())
      .totalRecords(0);

    when(searchInstancesClient.findInstances(query, true))
      .thenReturn(mockSearchResponse);

    Collection<BffSearchInstance> response = searchService.findInstances(query);
    assertThat(response, emptyIterable());
  }

  @Test
  void searchFindsInstanceWithNotItems() {
    String instanceId = UUID.randomUUID().toString();
    String query = "id=="  + UUID.randomUUID();

    SearchInstance searchInstance = new SearchInstance().id(instanceId).tenantId(TENANT_ID_CONSORTIUM);
    SearchInstances mockSearchResponse = new SearchInstances()
      .addInstancesItem(searchInstance)
      .totalRecords(1);

    when(searchInstancesClient.findInstances(query, true))
      .thenReturn(mockSearchResponse);

    BffSearchInstance bffSearchInstance = new BffSearchInstance().id(instanceId).tenantId(TENANT_ID_CONSORTIUM);
    when(searchInstanceMapper.toBffSearchInstanceWithoutItems(searchInstance))
      .thenReturn(bffSearchInstance);

    mockSystemUserScopedExecutionService();

    Instance instance = new Instance().id(instanceId).editions(Set.of("1st", "2st"));
    when(fetchingService.fetchByIds(any(), any(), any()))
      .thenReturn(List.of(instance));

    Collection<BffSearchInstance> response = searchService.findInstances(query);
    assertThat(response, equalTo(List.of(bffSearchInstance)));
    assertThat(response.stream().findFirst().orElseThrow().getEditions(),
      containsInAnyOrder("1st", "2st"));
  }

  @Test
  void itemFoundBySearchWasNotFoundInInventory() {
    String itemId = UUID.randomUUID().toString();
    String instanceId = UUID.randomUUID().toString();
    String query = "id=="  + UUID.randomUUID();

    SearchItem searchItem = new SearchItem()
      .id(itemId)
      .tenantId(TENANT_ID_CONSORTIUM);

    SearchInstance searchInstance = new SearchInstance()
      .id(instanceId)
      .tenantId(TENANT_ID_CONSORTIUM)
      .items(List.of(searchItem));

    SearchInstances mockSearchResponse = new SearchInstances()
      .addInstancesItem(searchInstance)
      .totalRecords(1);

    BffSearchInstance mockBffSearchInstance = new BffSearchInstance().id(instanceId).tenantId(TENANT_ID_CONSORTIUM);

    when(searchInstancesClient.findInstances(query, true))
      .thenReturn(mockSearchResponse);

    when(searchInstanceMapper.toBffSearchInstanceWithoutItems(searchInstance))
      .thenReturn(mockBffSearchInstance);
    mockSystemUserScopedExecutionService();

    Collection<BffSearchInstance> response = searchService.findInstances(query);
    BffSearchInstance bffSearchInstance = response.stream()
      .findFirst()
      .orElseThrow();

    assertThat(bffSearchInstance, is(mockBffSearchInstance));
    assertThat(bffSearchInstance.getItems(), emptyIterable());
  }

  @Test
  void duplicateItemIdHandledCorrectly() {
    String instanceId = UUID.randomUUID().toString();
    String itemId = UUID.randomUUID().toString();

    SearchItem searchItem1 = new SearchItem()
      .id(itemId)
      .tenantId(TENANT_ID_CONSORTIUM);
    SearchItem searchItem2 = new SearchItem()
      .id(itemId)
      .tenantId(TENANT_ID_CONSORTIUM);

    SearchInstance searchInstance = new SearchInstance()
      .id(instanceId)
      .tenantId(TENANT_ID_CONSORTIUM)
      .items(List.of(searchItem1, searchItem2));

    SearchInstances mockSearchResponse = new SearchInstances()
      .addInstancesItem(searchInstance)
      .totalRecords(1);

    BffSearchInstance mockBffSearchInstance = new BffSearchInstance()
      .id(instanceId)
      .tenantId(TENANT_ID_CONSORTIUM);
    when(searchInstanceMapper.toBffSearchInstanceWithoutItems(searchInstance))
      .thenReturn(mockBffSearchInstance);
    mockSystemUserScopedExecutionService();

    var query = "items.id==" + itemId;
    when(searchInstancesClient.findInstances(query, true))
      .thenReturn(mockSearchResponse);

    var response = searchService.findInstances(query);
    assertNotNull(response);
  }

  private void mockSystemUserScopedExecutionService() {
    when(executionService.executeSystemUserScoped(any(String.class), any(Callable.class)))
      .thenAnswer(invocation -> invocation.getArgument(1, Callable.class).call());
  }

}
