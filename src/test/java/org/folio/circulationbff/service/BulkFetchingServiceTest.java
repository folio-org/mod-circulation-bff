package org.folio.circulationbff.service;

import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.joining;
import static org.folio.circulationbff.service.BulkFetchingService.MAX_IDS_PER_QUERY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.folio.circulationbff.client.feign.GetByQueryParamsClient;
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
  private GetByQueryParamsClient<Collection<Integer>> getByQueryParamsClient;

  @Captor
  private ArgumentCaptor<Map<String, String>> queryParamsArgumentCaptor;

  @Test
  void fetchMultipleBatchesByIds() {
    Collection<Integer> firstPage = List.of(1, 2);
    Collection<Integer> secondPage = List.of(3, 4);

    when(getByQueryParamsClient.getByQueryParams(any(Map.class)))
      .thenReturn(firstPage, secondPage);

    List<String> ids = IntStream.range(0, MAX_IDS_PER_QUERY + 1)
      .boxed()
      .map(String::valueOf)
      .toList();

    Collection<Integer> result = new BulkFetchingServiceImpl()
      .fetchByIds(getByQueryParamsClient, ids, identity());

    assertThat(result, containsInAnyOrder(1, 2, 3, 4));
    verify(getByQueryParamsClient, times(2)).getByQueryParams(queryParamsArgumentCaptor.capture());
    List<Map<String, String>> actualQueryParams = queryParamsArgumentCaptor.getAllValues();
    String expectedQuery1 = idsToQuery("id", ids.subList(0, MAX_IDS_PER_QUERY));
    String expectedQuery2 = idsToQuery("id", ids.subList(MAX_IDS_PER_QUERY, ids.size()));

    assertThat(actualQueryParams, containsInAnyOrder(
      allOf(hasEntry("query", expectedQuery1), hasEntry("limit", String.valueOf(MAX_IDS_PER_QUERY))),
      allOf(hasEntry("query", expectedQuery2), hasEntry("limit", String.valueOf(ids.size() - MAX_IDS_PER_QUERY)))
    ));
  }

  @Test
  void fetchMultipleBatchesByUuidIndex() {
    Collection<Integer> firstPage = List.of(1, 2);
    Collection<Integer> secondPage = List.of(3, 4);

    when(getByQueryParamsClient.getByQueryParams(any(Map.class)))
      .thenReturn(firstPage, secondPage);

    List<String> ids = IntStream.range(0, MAX_IDS_PER_QUERY + 1)
      .boxed()
      .map(String::valueOf)
      .toList();

    Collection<Integer> result = new BulkFetchingServiceImpl()
      .fetchByUuidIndex(getByQueryParamsClient, "somethingId", ids, identity());

    assertThat(result, containsInAnyOrder(1, 2, 3, 4));
    verify(getByQueryParamsClient, times(2)).getByQueryParams(queryParamsArgumentCaptor.capture());
    List<Map<String, String>> actualQueryParams = queryParamsArgumentCaptor.getAllValues();
    String expectedQuery1 = idsToQuery("somethingId", ids.subList(0, MAX_IDS_PER_QUERY));
    String expectedQuery2 = idsToQuery("somethingId", ids.subList(MAX_IDS_PER_QUERY, ids.size()));

    assertThat(actualQueryParams, containsInAnyOrder(
      allOf(hasEntry("query", expectedQuery1), hasEntry("limit", String.valueOf(MAX_IDS_PER_QUERY))),
      allOf(hasEntry("query", expectedQuery2), hasEntry("limit", String.valueOf(ids.size() - MAX_IDS_PER_QUERY)))
    ));
  }

  @Test
  void fetchMultipleBatchesByUuidIndexAndAdditionalQueryParams() {
    Collection<Integer> firstPage = List.of(1, 2);
    Collection<Integer> secondPage = List.of(3, 4);

    when(getByQueryParamsClient.getByQueryParams(any(Map.class)))
      .thenReturn(firstPage, secondPage);

    List<String> ids = IntStream.range(0, MAX_IDS_PER_QUERY + 1)
      .boxed()
      .map(String::valueOf)
      .toList();

    Map<String, String> additionalQueryParams = Map.of("k1", "v1", "k2", "v2");
    Collection<Integer> result = new BulkFetchingServiceImpl()
      .fetchByUuidIndex(getByQueryParamsClient, "somethingId", ids, additionalQueryParams, identity());

    assertThat(result, containsInAnyOrder(1, 2, 3, 4));
    verify(getByQueryParamsClient, times(2)).getByQueryParams(queryParamsArgumentCaptor.capture());
    List<Map<String, String>> actualQueryParams = queryParamsArgumentCaptor.getAllValues();
    String expectedQuery1 = idsToQuery("somethingId", ids.subList(0, MAX_IDS_PER_QUERY));
    String expectedQuery2 = idsToQuery("somethingId", ids.subList(MAX_IDS_PER_QUERY, ids.size()));

    assertThat(actualQueryParams, containsInAnyOrder(
      allOf(hasEntry("query", expectedQuery1), hasEntry("k1", "v1"), hasEntry("k2", "v2")),
      allOf(hasEntry("query", expectedQuery2), hasEntry("k1", "v1"), hasEntry("k2", "v2"))
    ));
  }

  @Test
  void fetchByIdsDoesNothingWhenListOfIdsIsEmpty() {
    new BulkFetchingServiceImpl().fetchByIds(getByQueryParamsClient, emptyList(), identity());
    verify(getByQueryParamsClient, times(0)).getByQuery(any(CqlQuery.class), any(Integer.class));
  }

  private static <T> String idsToQuery(String index, Collection<T> ids) {
    return ids.stream()
      .map(id -> "\"" + id + "\"")
      .collect(joining(" or ", index + "==(", ")"));
  }

}
