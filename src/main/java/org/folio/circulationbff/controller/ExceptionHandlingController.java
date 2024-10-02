package org.folio.circulationbff.controller;

import static org.folio.circulationbff.util.ErrorHelper.ErrorCode.VALIDATION_ERROR;
import static org.folio.circulationbff.util.ErrorHelper.createExternalError;

import org.folio.circulationbff.domain.dto.Errors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import feign.FeignException;
import lombok.extern.log4j.Log4j2;

@RestControllerAdvice
@Log4j2
public class ExceptionHandlingController {

  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  @ExceptionHandler(FeignException.UnprocessableEntity.class)
  public Errors handleUnProcessableEntityErrors(Exception ex) {
    log.warn("Exception occurred ", ex);
    return createExternalError(ex.getMessage(), VALIDATION_ERROR);
  }
}
