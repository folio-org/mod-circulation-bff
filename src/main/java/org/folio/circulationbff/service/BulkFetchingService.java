package org.folio.circulationbff.service;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import org.folio.circulationbff.client.feign.GetByQueryParamsClient;

public interface BulkFetchingService {

  int MAX_IDS_PER_QUERY = 80;

  <C, E> Collection<E> fetchByIds(GetByQueryParamsClient<C> client, Collection<String> ids,
    Function<C, Collection<E>> collectionExtractor);
  <C, E> Map<String, E> fetchByIds(GetByQueryParamsClient<C> client, Collection<String> ids,
    Function<C, Collection<E>> collectionExtractor, Function<E, String> keyMapper);
  <C, E> Map<String, E> fetchByUuidIndex(GetByQueryParamsClient<C> client, String index,
    Collection<String> values, Function<C, Collection<E>> collectionExtractor, Function<E, String> keyMapper);
  <C, E> Collection<E> fetchByUuidIndex(GetByQueryParamsClient<C> client, String index,
    Collection<String> values, Function<C, Collection<E>> collectionExtractor);
  <C, E> Collection<E> fetchByUuidIndex(GetByQueryParamsClient<C> client, String index,
    Collection<String> values, Map<String, String> additionalQueryParams,
    Function<C, Collection<E>> collectionExtractor);
}
