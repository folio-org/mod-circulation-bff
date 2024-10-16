package org.folio.circulationbff.domain.dto;

import org.folio.circulationbff.support.EmptyJsonSerializer;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonTypeName("emptyBffSearchInstance")
@JsonSerialize(using = EmptyJsonSerializer.class)
public class EmptyBffSearchInstance extends BffSearchInstance {
}
