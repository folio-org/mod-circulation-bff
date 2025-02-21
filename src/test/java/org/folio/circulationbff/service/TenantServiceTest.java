package org.folio.circulationbff.service;

import static org.folio.circulationbff.service.impl.TenantServiceImpl.clearCache;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.folio.circulationbff.config.TenantConfig;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.folio.circulationbff.service.impl.TenantServiceImpl;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

  @Mock
  private UserTenantsService userTenantsService;

  @Mock
  private TenantConfig tenantConfig;

  @Mock
  private FolioExecutionContext folioExecutionContext;

  @InjectMocks
  private TenantServiceImpl tenantService;

  @BeforeEach
  void setUp() {
    clearCache();
  }

  @Test
  void getCurrentTenantIdReturnsTenantIdFromFolioContext() {
    when(folioExecutionContext.getTenantId())
      .thenReturn("test_tenant");
    assertEquals("test_tenant", tenantService.getCurrentTenantId());
  }

  @Test
  void centralTenantIdIsResolved() {
    when(folioExecutionContext.getTenantId())
      .thenReturn("tenant1");
    when(userTenantsService.getFirstUserTenant())
      .thenReturn(new UserTenant().centralTenantId("central_tenant"));

    // first invocation, cache is empty
    Optional<String> fetchedCentralTenantIdForTenant1 = tenantService.getCentralTenantId();
    assertTrue(fetchedCentralTenantIdForTenant1.isPresent());
    assertEquals("central_tenant", fetchedCentralTenantIdForTenant1.get());
    verify(userTenantsService, times(1)).getFirstUserTenant();

    // second invocation, central tenant ID is resolved from cache
    Optional<String> cachedCentralTenantIdForTenant1 = tenantService.getCentralTenantId();
    assertTrue(cachedCentralTenantIdForTenant1.isPresent());
    assertEquals("central_tenant", cachedCentralTenantIdForTenant1.get());
    verifyNoMoreInteractions(userTenantsService);

    // same for a different tenant
    when(folioExecutionContext.getTenantId())
      .thenReturn("tenant2");
    when(userTenantsService.getFirstUserTenant())
      .thenReturn(new UserTenant().centralTenantId("central_tenant"));

    Optional<String> fetchedCentralTenantIdForTenant2 = tenantService.getCentralTenantId();
    assertTrue(fetchedCentralTenantIdForTenant2.isPresent());
    assertEquals("central_tenant", fetchedCentralTenantIdForTenant2.get());
    verify(userTenantsService, times(2)).getFirstUserTenant();

    Optional<String> cachedCentralTenantIdForTenant2 = tenantService.getCentralTenantId();
    assertTrue(cachedCentralTenantIdForTenant2.isPresent());
    assertEquals("central_tenant", cachedCentralTenantIdForTenant2.get());
    verifyNoMoreInteractions(userTenantsService);
  }

  @Test
  void centralTenantIdIsNotResolved() {
    when(folioExecutionContext.getTenantId())
      .thenReturn("tenant1");
    when(userTenantsService.getFirstUserTenant())
      .thenReturn(null);

    // first invocation, cache is empty
    Optional<String> fetchedCentralTenant = tenantService.getCentralTenantId();
    assertTrue(fetchedCentralTenant.isEmpty());
    verify(userTenantsService, times(1)).getFirstUserTenant();

    // second invocation, central tenant ID is resolved from cache
    Optional<String> cachedCentralTenant = tenantService.getCentralTenantId();
    assertTrue(cachedCentralTenant.isEmpty());
    verifyNoMoreInteractions(userTenantsService);
  }

  @Test
  void secureTenantIdIsResolved() {
    when(tenantConfig.getSecureTenantId())
      .thenReturn("secure_tenant");

    Optional<String> secureTenantId = tenantService.getSecureTenantId();

    assertTrue(secureTenantId.isPresent());
    assertEquals("secure_tenant", secureTenantId.get());
  }

  @Test
  void secureTenantIdIsNotResolved() {
    when(tenantConfig.getSecureTenantId())
      .thenReturn(null);

    Optional<String> secureTenantId = tenantService.getSecureTenantId();

    assertTrue(secureTenantId.isEmpty());
  }

  @Test
  void isCurrentTenantCentralReturnsTrue() {
    when(folioExecutionContext.getTenantId())
      .thenReturn("central_tenant");
    when(userTenantsService.getFirstUserTenant())
      .thenReturn(new UserTenant().centralTenantId("central_tenant"));

    assertTrue(tenantService.isCurrentTenantCentral());
  }

  @Test
  void isCurrentTenantCentralReturnsFalseWhenTenantIdsDoNotMatch() {
    when(folioExecutionContext.getTenantId())
      .thenReturn("random_tenant");
    when(userTenantsService.getFirstUserTenant())
      .thenReturn(new UserTenant().centralTenantId("central_tenant"));

    assertFalse(tenantService.isCurrentTenantCentral());
  }

  @Test
  void isCurrentTenantCentralReturnsFalseWhenCentralTenantIdIsNotFound() {
    when(folioExecutionContext.getTenantId())
      .thenReturn("secure_tenant");
    when(userTenantsService.getFirstUserTenant())
      .thenReturn(null);

    assertFalse(tenantService.isCurrentTenantCentral());
  }

  @Test
  void isCurrentTenantSecureReturnsTrue() {
    when(folioExecutionContext.getTenantId())
      .thenReturn("secure_tenant");
    when(tenantConfig.getSecureTenantId())
      .thenReturn("secure_tenant");

    assertTrue(tenantService.isCurrentTenantSecure());
  }

  @Test
  void isCurrentTenantSecureReturnsFalseWhenTenantIdsDoNotMatch() {
    when(folioExecutionContext.getTenantId())
      .thenReturn("random_tenant");
    when(tenantConfig.getSecureTenantId())
      .thenReturn("secure_tenant");

    assertFalse(tenantService.isCurrentTenantSecure());
  }

  @Test
  void isCurrentTenantSecureReturnsFalseWhenSecureTenantIdIsNotFound() {
    when(folioExecutionContext.getTenantId())
      .thenReturn("secure_tenant");
    when(tenantConfig.getSecureTenantId())
      .thenReturn(null);

    assertFalse(tenantService.isCurrentTenantSecure());
  }
  
  @Test
  void isCentralTenantReturnsTrue() {
    when(folioExecutionContext.getTenantId())
      .thenReturn("random_tenant");
    when(userTenantsService.getFirstUserTenant())
      .thenReturn(new UserTenant().centralTenantId("central_tenant"));

    assertTrue(tenantService.isCentralTenant("central_tenant"));
  }

  @Test
  void isCentralTenantReturnsFalseTenantIdsDoNotMatch() {
    when(folioExecutionContext.getTenantId())
      .thenReturn("random_tenant");
    when(userTenantsService.getFirstUserTenant())
      .thenReturn(new UserTenant().centralTenantId("central_tenant"));

    assertFalse(tenantService.isCentralTenant("random_tenant"));
  }

  @Test
  void isCentralTenantReturnsFalseWhenCentralTenantIdIsNotFound() {
    when(folioExecutionContext.getTenantId())
      .thenReturn("random_tenant");
    when(userTenantsService.getFirstUserTenant())
      .thenReturn(null);

    assertFalse(tenantService.isCentralTenant("central_tenant"));
  }

  @Test
  void isSecureTenantReturnsTrue() {
    when(tenantConfig.getSecureTenantId())
      .thenReturn("secure_tenant");

    assertTrue(tenantService.isSecureTenant("secure_tenant"));
  }

  @Test
  void isSecureTenantReturnsFalseTenantIdsDoNotMatch() {
    when(tenantConfig.getSecureTenantId())
      .thenReturn("secure_tenant");

    assertFalse(tenantService.isSecureTenant("random_tenant"));
  }

  @Test
  void isSecureTenantReturnsFalseWhenSecureTenantIdIsNotFound() {
    when(tenantConfig.getSecureTenantId())
      .thenReturn(null);

    assertFalse(tenantService.isSecureTenant("secure_tenant"));
  }

}