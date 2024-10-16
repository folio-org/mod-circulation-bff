package org.folio.circulationbff.service;

import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static org.folio.circulationbff.service.BulkFetchingService.MAX_IDS_PER_QUERY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import org.folio.circulationbff.client.feign.GetByQueryClient;
import org.folio.circulationbff.service.impl.BulkFetchingServiceImpl;
import org.folio.circulationbff.support.CqlQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BulkFetchingServiceTest {

  @Mock
  private GetByQueryClient<Collection<Integer>> getByQueryClient;

  @Captor
  private ArgumentCaptor<CqlQuery> cqlQueryArgumentCaptor;

  @Test
  void fetchMultipleBatchesByIds() {
    Collection<Integer> firstPage = List.of(1, 2);
    Collection<Integer> secondPage = List.of(3, 4);

    when(getByQueryClient.getByQuery(any(CqlQuery.class)))
      .thenReturn(firstPage, secondPage);

    List<String> ids = IntStream.range(0, MAX_IDS_PER_QUERY + 1)
      .boxed()
      .map(String::valueOf)
      .toList();

    Collection<Integer> result = new BulkFetchingServiceImpl()
      .fetch(getByQueryClient, ids, identity());

    assertThat(result, containsInAnyOrder(1, 2, 3, 4));
    verify(getByQueryClient, times(2)).getByQuery(cqlQueryArgumentCaptor.capture());
    List<CqlQuery> actualQueries = cqlQueryArgumentCaptor.getAllValues();
    List<String> actualQueryStrings = actualQueries.stream().map(CqlQuery::query).toList();
    String expectedFirstQueryString = idsToQuery(ids.subList(0, MAX_IDS_PER_QUERY));
    String expectedSecondQueryString = idsToQuery(ids.subList(MAX_IDS_PER_QUERY, ids.size()));
    assertThat(actualQueryStrings,
      containsInAnyOrder(expectedFirstQueryString, expectedSecondQueryString));
  }

  @Test
  void fetchDoesNothingWhenListOfIdsIsEmpty() {
    new BulkFetchingServiceImpl().fetch(getByQueryClient, emptyList(), identity());
    verify(getByQueryClient, times(0)).getByQuery(any(CqlQuery.class));
  }

  private static <T> String idsToQuery(Collection<T> ids) {
    return ids.stream()
      .map(id -> "\"" + id + "\"")
      .collect(joining(" or ", "id==(", ")"));
  }

}