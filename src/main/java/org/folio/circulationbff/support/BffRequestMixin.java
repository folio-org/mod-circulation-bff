package org.folio.circulationbff.support;

import org.folio.circulationbff.domain.dto.EcsTlr;
import org.folio.circulationbff.domain.dto.Request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
  use = JsonTypeInfo.Id.DEDUCTION
)
@JsonSubTypes({
  @JsonSubTypes.Type(value = EcsTlr.class, name = "EcsTlr"),
  @JsonSubTypes.Type(value = Request.class, name = "Request")
})
public interface BffRequestMixin {
}
