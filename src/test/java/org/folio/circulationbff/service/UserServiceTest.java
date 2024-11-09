package org.folio.circulationbff.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import org.folio.circulationbff.domain.dto.User;
import org.folio.circulationbff.domain.dto.UserCollection;
import org.folio.circulationbff.service.impl.UsersServiceImpl;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

  private static final String EXTERNAL_SYSTEM_ID = "systemId";
  @Mock
  private SystemUserScopedExecutionService systemUserScopedExecutionService;
  @InjectMocks
  private UsersServiceImpl userService;

  @Test
  void getExternalUserTestShouldReturnEmptyCollection() {
    User expected = new User();
    expected.setExternalSystemId(EXTERNAL_SYSTEM_ID);
    UserCollection userCollection = new UserCollection();
    userCollection.setUsers(List.of(expected));
    when(systemUserScopedExecutionService.executeSystemUserScoped(anyString(), any()))
      .thenReturn(userCollection);
    User actual = userService.getExternalUser("externalUserId", "tenantId")
      .getUsers().get(0);
    assertThat(expected, is(actual));
  }
}
