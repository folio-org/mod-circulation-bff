package org.folio.circulationbff.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.folio.circulationbff.client.feign.SearchClient;
import org.folio.circulationbff.domain.dto.BffSearchInstance;
import org.folio.circulationbff.domain.dto.SearchInstance;
import org.folio.circulationbff.domain.dto.SearchInstances;
import org.folio.circulationbff.domain.mapping.SearchInstanceMapper;
import org.folio.circulationbff.service.impl.SearchServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {
  @Mock private SearchClient searchClient;
  @Mock private SearchInstanceMapper searchInstanceMapper;

  @InjectMocks
  private SearchServiceImpl searchService;

  @Test
  void instancesFoundSuccessfully() {
    String instanceId = UUID.randomUUID().toString();
    String query = "id=="  + UUID.randomUUID();

    SearchInstance searchInstance = new SearchInstance().id(instanceId);
    SearchInstances mockSearchResponse = new SearchInstances()
      .addInstancesItem(searchInstance)
      .totalRecords(1);

    when(searchClient.findInstances(query, true))
      .thenReturn(mockSearchResponse);

    BffSearchInstance bffSearchInstance = new BffSearchInstance().id(instanceId);
    when(searchInstanceMapper.toBffSearchInstance(searchInstance))
      .thenReturn(bffSearchInstance);

    Collection<BffSearchInstance> response = searchService.findInstances(query);
    assertThat(response, equalTo(List.of(bffSearchInstance)));
  }

}