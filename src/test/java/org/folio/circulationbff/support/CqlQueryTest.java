package org.folio.circulationbff.support;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

class CqlQueryTest {

  @Test
  void exactMatchAnyBuildsCorrectQuery() {
    assertThat(
      CqlQuery.exactMatchAny("key", List.of("value1", "value2")),
      is(new CqlQuery("key==(\"value1\" or \"value2\")"))
    );
  }

  @Test
  void exactMatchAnyThrowsExceptionWhenCollectionOfValuesIsEmpty() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
      () -> CqlQuery.exactMatchAny("index", emptyList()));
    assertThat(exception.getMessage(), is("Values cannot be null or empty"));
  }

  @Test
  void exactMatchAnyThrowsExceptionWhenCollectionOfValuesIsNull() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
      () -> CqlQuery.exactMatchAny("index", null));
    assertThat(exception.getMessage(), is("Values cannot be null or empty"));
  }

  @Test
  void exactMatchAnyThrowsExceptionWhenIndexIsNull() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
      () -> CqlQuery.exactMatchAny(null, List.of("value")));
    assertThat(exception.getMessage(), is("Index cannot be blank"));
  }

  @Test
  void exactMatchAnyThrowsExceptionWhenIndexIsEmptyString() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
      () -> CqlQuery.exactMatchAny("", List.of("value")));
    assertThat(exception.getMessage(), is("Index cannot be blank"));
  }

}