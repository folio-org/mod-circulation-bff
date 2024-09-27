package org.folio.circulationbff.domain.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllowedServicePointParams {
  private String operation;

  private UUID patronGroupId;

  private UUID instanceId;

  private UUID requestId;

  private UUID requesterId;

  private UUID itemId;
}
