package org.folio.circulationbff.exception;

public class UserTenantException extends RuntimeException{
  public UserTenantException(String message) {
    super(message);
  }
}
