package org.folio.circulationbff.service.impl;

import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.folio.circulationbff.client.feign.GetByQueryClient;
import org.folio.circulationbff.service.BulkFetchingService;
import org.folio.circulationbff.support.CqlQuery;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RequiredArgsConstructor
@Service
@Log4j2
public class
BulkFetchingServiceImpl implements BulkFetchingService {

  @Override public <C, E> Collection<E> fetch(GetByQueryClient<C> client, Collection<String> ids,
    Function<C, Collection<E>> collectionExtractor) {

    return getAsStream(client, ids, collectionExtractor)
      .toList();
  }

  @Override public <C, E> Map<String, E> fetch(GetByQueryClient<C> client, Collection<String> ids,
    Function<C, Collection<E>> collectionExtractor, Function<E, String> keyMapper) {

    return getAsStream(client, ids, collectionExtractor)
      .collect(toMap(keyMapper, identity()));
  }

  private <C, E> Stream<E> getAsStream(GetByQueryClient<C> client, Collection<String> ids,
    Function<C, Collection<E>> collectionExtractor) {

    if (ids.isEmpty()) {
      log.info("getAsStream:: provided collection of IDs is empty, fetching nothing");
      return Stream.empty();
    }
    log.info("getAsStream:: fetching objects by {} IDs", ids.size());
    log.debug("getAsStream:: ids={}", ids);

    return Lists.partition(new ArrayList<>(ids), MAX_IDS_PER_QUERY)
      .stream()
      .map(batch -> fetchByIds(batch, client))
      .map(collectionExtractor)
      .peek(batch -> log.info("getAsStream:: found {} objects", batch::size))
      .flatMap(Collection::stream);
  }

  private <T> T fetchByIds(Collection<String> ids, GetByQueryClient<T> client) {
    log.info("fetchByIds:: fetching a batch of {} IDs", ids::size);
    CqlQuery query = CqlQuery.exactMatchAnyId(ids);
    log.debug("fetchByIds:: query: {}", query);

    return client.getByQuery(query, ids.size());
  }

}
