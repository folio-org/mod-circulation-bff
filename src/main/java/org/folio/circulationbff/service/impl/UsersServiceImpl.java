package org.folio.circulationbff.service.impl;

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
  public User getExternalUser(String externalUserId, String tenantId) {
    log.info("getUser:: userId = {}, tenantId = {}", externalUserId, tenantId);
    User user = systemUserScopedExecutionService.executeSystemUserScoped(tenantId,
      () -> client.getExternalUserByQuery(String.format(USER_BY_ID_QUERY, externalUserId)));
    log.info("user: {}", user);
    return user;
  }
}
