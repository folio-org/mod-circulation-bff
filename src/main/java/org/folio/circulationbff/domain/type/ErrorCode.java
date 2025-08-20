package org.folio.circulationbff.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  FAILED_TO_DECLARE_CLAIMED_RETURNED_ITEM_AS_MISSING(
    "FAILED_TO_DECLARE_CLAIMED_RETURNED_ITEM_AS_MISSING");

  private final String value;
}
