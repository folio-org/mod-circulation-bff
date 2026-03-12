package org.folio.circulationbff.domain.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.folio.circulationbff.support.EmptyJsonSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

@JsonTypeName("emptyBffSearchInstance")
@JsonSerialize(using = EmptyJsonSerializer.class)
public class EmptyBffSearchInstance extends BffSearchInstance {
}
