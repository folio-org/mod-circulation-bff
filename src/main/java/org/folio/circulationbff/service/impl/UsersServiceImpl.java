package org.folio.circulationbff.service.impl;

import org.folio.circulationbff.client.feign.UserClient;
import org.folio.circulationbff.domain.dto.User;
import org.folio.circulationbff.domain.dto.UserCollection;
import org.folio.circulationbff.service.UserService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class UsersServiceImpl implements UserService {

  private static final String USER_BY_EXTERNAL_SYSTEM_ID_QUERY = "externalSystemId==%s";
  private final UserClient userClient;
  private final SystemUserScopedExecutionService systemUserScopedExecutionService;

  @Override
  public User find(String userId) {
    log.info("find:: looking up user {}", userId);
    return userClient.getUser(userId);
  }

  @Override
  public UserCollection getExternalUser(String externalUserId, String tenantId) {
    log.info("getExternalUser:: externalUserId = {}, tenantId = {}", externalUserId,
      tenantId);

    return systemUserScopedExecutionService.executeSystemUserScoped(tenantId,
      () -> userClient.getUsersByQuery(String.format(USER_BY_EXTERNAL_SYSTEM_ID_QUERY, externalUserId)));
  }

}
