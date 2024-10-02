package org.folio.circulationbff.controller;

import static org.folio.circulationbff.util.ErrorHelper.ErrorCode.VALIDATION_ERROR;
import static org.folio.circulationbff.util.ErrorHelper.createExternalError;

import org.folio.circulationbff.domain.dto.Errors;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import feign.FeignException;
import lombok.extern.log4j.Log4j2;

@RestControllerAdvice
@Log4j2
public class ExceptionHandlingController {

  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  @ExceptionHandler(FeignException.UnprocessableEntity.class)
  public Errors handleUnProcessableEntityErrors(Exception ex) {
    logExceptionMessage(ex);
    return createExternalError(ex.getMessage(), VALIDATION_ERROR);
  }

  private void logExceptionMessage(Exception ex) {
    log.warn("Exception occurred ", ex);
  }

}
