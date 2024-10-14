package org.folio.circulationbff.service;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import org.folio.circulationbff.client.feign.GetByQueryClient;

public interface BulkFetchingService {
  <C, E> Collection<E> fetch(GetByQueryClient<C> client, Collection<String> ids,
    Function<C, Collection<E>> collectionExtractor);

  <C, E> Map<String, E> fetch(GetByQueryClient<C> client, Collection<String> ids,
    Function<C, Collection<E>> collectionExtractor, Function<E, String> keyMapper);
}
