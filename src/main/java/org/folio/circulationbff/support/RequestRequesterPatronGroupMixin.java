package org.folio.circulationbff.support;

import org.folio.circulationbff.domain.dto.RequestRequesterPatronGroupOneOf;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
  use = JsonTypeInfo.Id.DEDUCTION
)
@JsonSubTypes({
  @JsonSubTypes.Type(value = RequestRequesterPatronGroupOneOf.class, name = "RequestRequesterPatronGroupOneOf"),
  @JsonSubTypes.Type(value = String.class, name = "string")
})
public interface RequestRequesterPatronGroupMixin {
}
