package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.UserCollection;

public interface UserService {

  UserCollection getExternalUser(String externalUserId, String tenantId);
}
