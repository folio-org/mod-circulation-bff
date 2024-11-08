package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.User;

public interface UserService {

  User getExternalUser(String externalUserId, String tenantId);
}
