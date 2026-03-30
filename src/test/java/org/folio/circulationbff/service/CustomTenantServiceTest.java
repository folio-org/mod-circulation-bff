package org.folio.circulationbff.service;

import static org.mockito.Mockito.verifyNoInteractions;

import org.folio.circulationbff.service.impl.CustomTenantService;
import org.folio.spring.FolioExecutionContext;
import org.folio.spring.service.PrepareSystemUserService;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomTenantServiceTest {

  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private PrepareSystemUserService prepareSystemUserService;
  @InjectMocks
  private CustomTenantService customTenantService;

  @Test
  void deleteTenantDoesNothing() {
    customTenantService.deleteTenant(new TenantAttributes());
    verifyNoInteractions(prepareSystemUserService);
  }
}
