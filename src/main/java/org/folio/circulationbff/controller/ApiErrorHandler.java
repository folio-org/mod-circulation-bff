package org.folio.circulationbff.controller;

import java.util.List;

import org.folio.circulationbff.domain.dto.ErrorResponse;
import org.folio.circulationbff.domain.dto.Parameter;
import org.folio.circulationbff.domain.type.ErrorCode;
import org.folio.circulationbff.domain.dto.Error;
import org.folio.circulationbff.exception.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiErrorHandler {

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ErrorResponse> handleValidationExceptions(ValidationException e) {
    return buildSingleErrorResponseEntity(HttpStatus.UNPROCESSABLE_ENTITY,
      buildError(e, e.getCode(), e.getParameters()));
  }

  private static ResponseEntity<ErrorResponse> buildSingleErrorResponseEntity(
    HttpStatusCode httpStatusCode, Error error) {

    return buildResponseEntity(httpStatusCode, new ErrorResponse()
      .errors(List.of(error))
      .totalRecords(1));
  }

  private static ResponseEntity<ErrorResponse> buildResponseEntity(HttpStatusCode status,
    ErrorResponse errorResponse) {

    return ResponseEntity.status(status).body(errorResponse);
  }

  private static Error buildError(Exception e, ErrorCode code, List<Parameter> parameters) {
    return new Error()
      .message(e.getMessage())
      .code(code.getValue())
      .parameters(parameters);
  }
}
