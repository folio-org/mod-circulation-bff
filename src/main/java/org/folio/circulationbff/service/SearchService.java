package org.folio.circulationbff.service;

import java.util.Collection;

import org.folio.circulationbff.domain.dto.BffSearchInstance;
import org.folio.circulationbff.domain.dto.ConsortiumItem;

public interface SearchService {
  Collection<BffSearchInstance> findInstances(String query);
  ConsortiumItem findConsortiumItem(String itemId);
}
