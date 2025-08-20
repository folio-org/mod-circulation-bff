package org.folio.circulationbff.exception;

import java.util.List;

import org.folio.circulationbff.domain.dto.Parameter;
import org.folio.circulationbff.domain.type.ErrorCode;

import lombok.Getter;

@Getter
public class ValidationException extends RuntimeException {
  private final ErrorCode code;
  private final transient List<Parameter> parameters;

  public ValidationException(String message, ErrorCode code, List<Parameter> parameters) {

    super(message);
    this.code = code;
    this.parameters = parameters;
  }
}
