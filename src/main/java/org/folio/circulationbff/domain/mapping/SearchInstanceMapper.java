package org.folio.circulationbff.domain.mapping;

import org.folio.circulationbff.domain.dto.BffSearchInstance;
import org.folio.circulationbff.domain.dto.SearchInstance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SearchInstanceMapper {

  @Mapping(target = "items", ignore = true)
  BffSearchInstance toBffSearchInstance(SearchInstance searchInstance);
}
