package org.folio.circulationbff.service;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.folio.circulationbff.client.feign.SearchClient;
import org.folio.circulationbff.domain.dto.BffSearchInstance;
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
  @Mock private SearchClient searchClient;
  @Mock private SearchInstanceMapper searchInstanceMapper;
  @Mock private SystemUserScopedExecutionService executionService;

  @InjectMocks
  private SearchServiceImpl searchService;

  @Test
  void searchFindsNoInstances() {
    String instanceId = UUID.randomUUID().toString();
    String query = "id==" + instanceId;

    SearchInstances mockSearchResponse = new SearchInstances()
      .instances(emptyList())
      .totalRecords(0);

    when(searchClient.findInstances(query, true))
      .thenReturn(mockSearchResponse);

    Collection<BffSearchInstance> response = searchService.findInstances(query);
    assertThat(response, emptyIterable());
  }

  @Test
  void searchFindsInstanceWithNotItems() {
    String instanceId = UUID.randomUUID().toString();
    String query = "id=="  + UUID.randomUUID();

    SearchInstance searchInstance = new SearchInstance().id(instanceId);
    SearchInstances mockSearchResponse = new SearchInstances()
      .addInstancesItem(searchInstance)
      .totalRecords(1);

    when(searchClient.findInstances(query, true))
      .thenReturn(mockSearchResponse);

    BffSearchInstance bffSearchInstance = new BffSearchInstance().id(instanceId);
    when(searchInstanceMapper.toBffSearchInstanceWithoutItems(searchInstance))
      .thenReturn(bffSearchInstance);

    Collection<BffSearchInstance> response = searchService.findInstances(query);
    assertThat(response, equalTo(List.of(bffSearchInstance)));
  }

  @Test
  void itemFoundBySearchWasNotFoundInInventory() {
    String itemId = UUID.randomUUID().toString();
    String instanceId = UUID.randomUUID().toString();
    String query = "id=="  + UUID.randomUUID();

    SearchItem searchItem = new SearchItem()
      .id(itemId)
      .tenantId("test_tenant");

    SearchInstance searchInstance = new SearchInstance()
      .id(instanceId)
      .items(List.of(searchItem));

    SearchInstances mockSearchResponse = new SearchInstances()
      .addInstancesItem(searchInstance)
      .totalRecords(1);

    BffSearchInstance mockBffSearchInstance = new BffSearchInstance().id(instanceId);

    when(searchClient.findInstances(query, true))
      .thenReturn(mockSearchResponse);

    when(executionService.executeSystemUserScoped(any(String.class), any()))
      .thenReturn(emptyList());

    when(searchInstanceMapper.toBffSearchInstanceWithoutItems(searchInstance))
      .thenReturn(mockBffSearchInstance);

    Collection<BffSearchInstance> response = searchService.findInstances(query);
    BffSearchInstance bffSearchInstance = response.stream()
      .findFirst()
      .orElseThrow();

    assertThat(bffSearchInstance, is(mockBffSearchInstance));
    assertThat(bffSearchInstance.getItems(), emptyIterable());
  }

}