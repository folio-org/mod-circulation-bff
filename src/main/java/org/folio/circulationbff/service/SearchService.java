package org.folio.circulationbff.service;

import java.util.Collection;
import java.util.Optional;

import org.folio.circulationbff.domain.dto.BffSearchInstance;
import org.folio.circulationbff.domain.dto.ConsortiumItem;
import org.folio.circulationbff.domain.dto.SearchInstance;

public interface SearchService {
  SearchInstance findInstanceByItemId(String itemId);
  Optional<SearchInstance> findInstanceByItemBarcode(String itemBarcode);
  Collection<BffSearchInstance> findInstances(String idIndex, Collection<String> values);
  Collection<BffSearchInstance> findInstances(String query);
  ConsortiumItem findConsortiumItem(String itemId);
}
