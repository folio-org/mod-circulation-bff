package org.folio.circulationbff.support;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public abstract class RequestRequesterMixin {
  @JsonDeserialize(using = PatronGroupDeserializer.class)
  private Object patronGroup;
}
