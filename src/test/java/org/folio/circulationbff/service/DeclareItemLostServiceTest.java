package org.folio.circulationbff.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.client.feign.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.DeclareItemLostRequest;
import org.folio.circulationbff.service.impl.DeclareItemLostServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

class DeclareItemLostServiceTest {

  @Mock
  private SettingsService settingsService;
  @Mock
  private TenantService tenantService;
  @Mock
  private EcsTlrClient ecsTlrClient;
  @Mock
  private CirculationClient circulationClient;
  @Mock
  private RequestMediatedClient requestMediatedClient;
  @InjectMocks
  private DeclareItemLostServiceImpl service;

  private final UUID loanId = UUID.randomUUID();
  private final DeclareItemLostRequest request = new DeclareItemLostRequest();
  private final String tenantId = "tenant";
  private final ResponseEntity<Void> response = ResponseEntity.ok().build();

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(tenantService.getCurrentTenantId()).thenReturn(tenantId);
  }

  @Test
  void shouldUseCirculationClientWhenEcsTlrFeatureDisabled() {
    when(settingsService.isEcsTlrFeatureEnabled(tenantId)).thenReturn(false);
    when(circulationClient.declareItemLost(loanId, request)).thenReturn(response);

    ResponseEntity<Void> result = service.declareItemLost(loanId, request);

    assertEquals(response, result);
    verify(circulationClient).declareItemLost(loanId, request);
    verifyNoMoreInteractions(ecsTlrClient, requestMediatedClient);
  }

  @Test
  void shouldUseEcsTlrClientWhenCentralTenant() {
    when(settingsService.isEcsTlrFeatureEnabled(tenantId)).thenReturn(true);
    when(tenantService.isCentralTenant(tenantId)).thenReturn(true);
    when(ecsTlrClient.declareItemLost(loanId, request)).thenReturn(response);

    ResponseEntity<Void> result = service.declareItemLost(loanId, request);

    assertEquals(response, result);
    verify(ecsTlrClient).declareItemLost(loanId, request);
    verifyNoMoreInteractions(circulationClient, requestMediatedClient);
  }

  @Test
  void shouldUseRequestMediatedClientWhenCurrentTenantSecure() {
    when(settingsService.isEcsTlrFeatureEnabled(tenantId)).thenReturn(true);
    when(tenantService.isCentralTenant(tenantId)).thenReturn(false);
    when(tenantService.isCurrentTenantSecure()).thenReturn(true);
    when(requestMediatedClient.declareItemLost(loanId, request)).thenReturn(response);

    ResponseEntity<Void> result = service.declareItemLost(loanId, request);

    assertEquals(response, result);
    verify(requestMediatedClient).declareItemLost(loanId, request);
    verifyNoMoreInteractions(circulationClient, ecsTlrClient);
  }

  @Test
  void shouldFallbackToCirculationClientWhenNotCentralOrSecure() {
    when(settingsService.isEcsTlrFeatureEnabled(tenantId)).thenReturn(true);
    when(tenantService.isCentralTenant(tenantId)).thenReturn(false);
    when(tenantService.isCurrentTenantSecure()).thenReturn(false);
    when(circulationClient.declareItemLost(loanId, request)).thenReturn(response);

    ResponseEntity<Void> result = service.declareItemLost(loanId, request);

    assertEquals(response, result);
    verify(circulationClient).declareItemLost(loanId, request);
    verifyNoMoreInteractions(ecsTlrClient, requestMediatedClient);
  }
}
