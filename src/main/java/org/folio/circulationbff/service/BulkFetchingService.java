package org.folio.circulationbff.service;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import org.folio.circulationbff.client.feign.GetByQueryClient;

public interface BulkFetchingService {

  int MAX_IDS_PER_QUERY = 80;

  <C, E> Collection<E> fetchByIds(GetByQueryClient<C> client, Collection<String> ids,
    Function<C, Collection<E>> collectionExtractor);

  <C, E> Map<String, E> fetchByIds(GetByQueryClient<C> client, Collection<String> ids,
    Function<C, Collection<E>> collectionExtractor, Function<E, String> keyMapper);

  <C, E> Collection<E> fetchByUuidIndex(GetByQueryClient<C> client, Collection<String> values,
    String idIndex, Function<C, Collection<E>> collectionExtractor);

  <C, E> Map<String, E> fetchByUuidIndex(GetByQueryClient<C> client, Collection<String> values,
    String idIndex, Function<C, Collection<E>> collectionExtractor, Function<E, String> keyMapper);
}
