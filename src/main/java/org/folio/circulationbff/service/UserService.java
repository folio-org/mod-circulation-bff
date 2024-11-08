package org.folio.circulationbff.service;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.User;

public interface UserService {

  User getUser(String userId, String tenantId);
}
