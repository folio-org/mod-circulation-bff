package org.folio.circulationbff.service.impl;

import java.util.UUID;

import org.folio.circulationbff.client.feign.UserClient;
import org.folio.circulationbff.domain.dto.User;
import org.folio.circulationbff.service.UserService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class UsersServiceImpl implements UserService {

  private static final String USER_BY_ID_QUERY = "externalSystemId==%s";
  private final UserClient client;
  private final SystemUserScopedExecutionService systemUserScopedExecutionService;

  @Override
  public User getUser(UUID userId, String tenantId) {
    log.info("getUser:: userId = {}, tenantId = {}", userId, tenantId);
    return systemUserScopedExecutionService.executeSystemUserScoped(tenantId,
      () -> client.getUserByQuery(String.format(USER_BY_ID_QUERY, userId)));
  }
}
