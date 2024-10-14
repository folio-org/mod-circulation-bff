package org.folio.circulationbff.service;

import java.util.List;

import org.folio.circulationbff.domain.dto.BffSearchInstance;

public interface SearchService {
  List<BffSearchInstance> findInstances(String query);
}
