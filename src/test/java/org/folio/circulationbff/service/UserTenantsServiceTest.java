package org.folio.circulationbff.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Stream;

import org.folio.circulationbff.client.feign.UserTenantsClient;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.folio.circulationbff.domain.dto.UserTenantCollection;
import org.folio.circulationbff.service.impl.UserTenantsServiceImpl;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserTenantsServiceTest {
  private static final String CENTRAL_TENANT_ID = "consortium";
  private static final String TENANT_ID = "university";

  @Mock
  private UserTenantsClient userTenantsClient;

  @InjectMocks
  private UserTenantsServiceImpl userTenantsService;

  @ParameterizedTest
  @MethodSource("userTenantCollectionToExpectedValue")
  void isCentralTenantIdTest(UserTenantCollection userTenantCollection, boolean expectedValue) {
    when(userTenantsClient.getUserTenants(anyInt())).thenReturn(userTenantCollection);

    assertThat(userTenantsService.isCentralTenant(), equalTo(expectedValue));
  }

  private static Stream<Arguments> userTenantCollectionToExpectedValue() {
    return Stream.of(
      Arguments.of(buildCollection(TENANT_ID), false),
      Arguments.of(buildCollection(CENTRAL_TENANT_ID), true),
      Arguments.of(buildCollection(null), false),
      Arguments.of(null, false),
      Arguments.of(new UserTenantCollection().addUserTenantsItem(null), false)
    );
  }

  private static UserTenantCollection buildCollection(String tenantId) {
    UserTenant userTenant = new UserTenant();
    userTenant.setCentralTenantId(CENTRAL_TENANT_ID);
    userTenant.setTenantId(tenantId);
    return new UserTenantCollection(List.of(userTenant), 1);
  }
}
