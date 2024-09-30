package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.InstanceSearchResult;

public interface SearchService {
  InstanceSearchResult findInstances(String query);
}
