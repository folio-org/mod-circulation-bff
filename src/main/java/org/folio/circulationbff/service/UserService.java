package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.User;
import org.folio.circulationbff.domain.dto.UserCollection;

public interface UserService {

  User find(String userId);

  UserCollection getExternalUser(String externalUserId, String tenantId);

}
