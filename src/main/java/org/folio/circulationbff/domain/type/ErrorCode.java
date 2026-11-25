package org.folio.circulationbff.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  IN_HOUSE_USE_NOT_SUPPORTED_IN_CENTRAL_TENANT(
    "IN_HOUSE_USE_NOT_SUPPORTED_IN_CENTRAL_TENANT"),
  FAILED_TO_DECLARE_CLAIMED_RETURNED_ITEM_AS_MISSING(
    "FAILED_TO_DECLARE_CLAIMED_RETURNED_ITEM_AS_MISSING");

  private final String value;
}
