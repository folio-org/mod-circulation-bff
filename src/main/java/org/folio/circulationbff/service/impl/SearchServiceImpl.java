package org.folio.circulationbff.service.impl;

import org.folio.circulationbff.client.SearchClient;
import org.folio.circulationbff.domain.dto.InstanceSearchResult;
import org.folio.circulationbff.service.SearchService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

  private final SearchClient searchClient;

  @Override
  public InstanceSearchResult findInstances(String query) {
    log.info("findInstances:: searching instances by query: {}", query);
    return searchClient.findInstances(query, true);
  }
}
