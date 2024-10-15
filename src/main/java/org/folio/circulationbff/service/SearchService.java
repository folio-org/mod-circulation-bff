package org.folio.circulationbff.service;

import java.util.Collection;

import org.folio.circulationbff.domain.dto.BffSearchInstance;

public interface SearchService {
  Collection<BffSearchInstance> findInstances(String query);
}
