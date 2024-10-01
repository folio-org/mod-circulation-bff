package org.folio.circulationbff.controller;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.Instance;
import org.folio.circulationbff.domain.dto.InstanceSearchResult;
import org.folio.circulationbff.service.SearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class CirculationBffControllerTest {

  @Mock
  private SearchService searchService;

  @InjectMocks
  private CirculationBffController controller;

  @Test
  void instancesFoundSuccessfully() {
    String query = "id=="  + UUID.randomUUID();
    InstanceSearchResult mockSearchResponse = new InstanceSearchResult()
      .addInstancesItem(new Instance().id(UUID.randomUUID().toString()))
      .totalRecords(1);
    when(searchService.findInstances(query))
      .thenReturn(mockSearchResponse);
    var response = controller.circulationBffRequestsSearchInstancesGet(query);
    assertThat(response.getStatusCode(), is(HttpStatus.OK));
    assertThat(response.getBody(), equalTo(mockSearchResponse));
  }

}