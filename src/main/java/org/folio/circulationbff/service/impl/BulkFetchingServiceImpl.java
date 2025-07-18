package org.folio.circulationbff.service.impl;

import static java.util.function.UnaryOperator.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

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
public class BulkFetchingServiceImpl implements BulkFetchingService {

  @Override
  public <C, E> Collection<E> fetchByIds(GetByQueryClient<C> client, Collection<String> ids,
    Function<C, Collection<E>> collectionExtractor) {

    return fetchByUuidIndex(client, ids, "id", collectionExtractor);
  }

  @Override
  public <C, E> Map<String, E> fetchByIds(GetByQueryClient<C> client, Collection<String> ids,
    Function<C, Collection<E>> collectionExtractor, Function<E, String> keyMapper) {

    return fetchByIds(client, ids, collectionExtractor)
      .stream()
      .collect(toMap(keyMapper, identity()));
  }

  @Override
  public <C, E> Map<String, E> fetchByUuidIndex(GetByQueryClient<C> client, Collection<String> values,
    String index, Function<C, Collection<E>> collectionExtractor, Function<E, String> keyMapper) {

    return fetchByUuidIndex(client, values, index, collectionExtractor)
      .stream()
      .collect(toMap(keyMapper, identity()));
  }

  @Override
  public <C, E> Collection<E> fetchByUuidIndex(GetByQueryClient<C> client, Collection<String> values,
    String index, Function<C, Collection<E>> collectionExtractor) {

    if (values.isEmpty()) {
      log.info("fetchByUuidIndex:: provided collection of UUIDs is empty, fetching nothing");
      return new ArrayList<>();
    }

    log.info("fetchByUuidIndex:: fetching by {} values for index {}", values.size(), index);
    log.debug("fetchByUuidIndex:: values={}", values);

    Collection<E> result = Lists.partition(new ArrayList<>(values), MAX_IDS_PER_QUERY)
      .stream()
      .map(batch -> fetchByUuidIndex(batch, index, client))
      .map(collectionExtractor)
      .flatMap(Collection::stream)
      .collect(toList());

    log.info("fetchByUuidIndex:: fetched {} objects", result::size);
    return result;
  }

  private <T> T fetchByUuidIndex(Collection<String> ids, String index, GetByQueryClient<T> client) {
    log.info("fetchByUuidIndex:: fetching by a batch of {} UUIDs", ids::size);
    CqlQuery query = CqlQuery.exactMatchAny(index, ids);
    log.debug("fetchByUuidIndex:: query: {}", query);

    return client.getByQuery(query, ids.size());
  }

}
