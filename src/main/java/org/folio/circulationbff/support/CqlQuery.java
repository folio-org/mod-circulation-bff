package org.folio.circulationbff.support;

import static java.lang.String.format;

import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

public record CqlQuery(String query) {

  public static CqlQuery exactMatch(String index, String value) {
    return new CqlQuery(format("%s==\"%s\"", index, value));
  }

  public static CqlQuery exactMatchAnyId(Collection<String> values) {
    return exactMatchAny("id", values);
  }

  public static CqlQuery exactMatchAny(String index, Collection<String> values) {
    if (StringUtils.isBlank(index)) {
      throw new IllegalArgumentException("Index cannot be blank");
    }
    if (values == null || values.isEmpty()) {
      throw new IllegalArgumentException("Values cannot be null or empty");
    }

    String joinedValues = values.stream()
      .map(value -> StringUtils.wrap(value, "\""))
      .collect(Collectors.joining(" or "));

    return new CqlQuery(format("%s==(%s)", index, joinedValues));
  }

  @Override
  public String toString() {
    return query;
  }

}
