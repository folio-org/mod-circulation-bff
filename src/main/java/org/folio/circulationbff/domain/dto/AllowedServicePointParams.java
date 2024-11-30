package org.folio.circulationbff.domain.dto;

import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AllowedServicePointParams {
  private String operation;

  private UUID patronGroupId;

  private UUID instanceId;

  private UUID requestId;

  private UUID requesterId;

  private UUID itemId;
}
