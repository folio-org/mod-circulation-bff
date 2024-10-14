package org.folio.circulationbff.service;

import org.folio.circulationbff.client.feign.SearchClient;
import org.folio.circulationbff.service.impl.SearchServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

  @Mock
  private SearchClient searchClient;

  @InjectMocks
  private SearchServiceImpl searchService;

  @Test
  void instancesFoundSuccessfully() {
//    String query = "id=="  + UUID.randomUUID();
//    InstanceSearchResult mockSearchResponse = new InstanceSearchResult()
//      .addInstancesItem(new SearchInstance().id(UUID.randomUUID().toString()))
//      .totalRecords(1);
//    when(searchClient.findInstances(query, true))
//      .thenReturn(mockSearchResponse);
//    InstanceSearchResult response = searchService.findInstances(query);
//    assertThat(response, Matchers.equalTo(mockSearchResponse));
  }

}