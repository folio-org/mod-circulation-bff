package org.folio.circulationbff.service;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.AllowedServicePoints;

public interface CirculationBffService {
  AllowedServicePoints getAllowedServicePoints(UUID patronGroupId, String operation,
    UUID instanceId, UUID requestId, UUID requesterId, UUID itemId);
}
