package org.folio.circulationbff.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.client.feign.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.DeclareClaimedReturnedItemAsMissingRequest;
import org.folio.circulationbff.domain.dto.TlrDeclareClaimedReturnedItemAsMissingRequest;
import org.folio.circulationbff.exception.ValidationException;
import org.folio.circulationbff.service.impl.DeclareClaimedReturnedItemAsMissingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

class DeclareClaimedReturnedItemAsMissingServiceTest {

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
  private DeclareClaimedReturnedItemAsMissingServiceImpl service;

  private final UUID loanId = UUID.randomUUID();
  private final DeclareClaimedReturnedItemAsMissingRequest request =
    new DeclareClaimedReturnedItemAsMissingRequest();
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
    when(circulationClient.declareClaimedReturnedItemAsMissing(loanId, request)).thenReturn(response);

    service.declareClaimedReturnedItemAsMissing(loanId, request);

    verify(circulationClient).declareClaimedReturnedItemAsMissing(loanId, request);
    verifyNoMoreInteractions(ecsTlrClient, requestMediatedClient);
  }

  @Test
  void shouldUseEcsTlrClientWhenCentralTenant() {
    var tlrRequest = new TlrDeclareClaimedReturnedItemAsMissingRequest().loanId(loanId);
    when(settingsService.isEcsTlrFeatureEnabled(tenantId)).thenReturn(true);
    when(tenantService.isCentralTenant(tenantId)).thenReturn(true);
    when(ecsTlrClient.declareClaimedReturnedItemAsMissing(tlrRequest)).thenReturn(response);

    service.declareClaimedReturnedItemAsMissing(loanId, request);

    verify(ecsTlrClient).declareClaimedReturnedItemAsMissing(tlrRequest);
    verifyNoMoreInteractions(circulationClient, requestMediatedClient);
  }

  @Test
  void shouldUseRequestMediatedClientWhenCurrentTenantSecure() {
    when(settingsService.isEcsTlrFeatureEnabled(tenantId)).thenReturn(true);
    when(tenantService.isCentralTenant(tenantId)).thenReturn(false);
    when(tenantService.isSecureTenant(tenantId)).thenReturn(true);
    when(requestMediatedClient.declareClaimedReturnedItemAsMissing(loanId, request)).thenReturn(response);

    service.declareClaimedReturnedItemAsMissing(loanId, request);

    verify(requestMediatedClient).declareClaimedReturnedItemAsMissing(loanId, request);
    verifyNoMoreInteractions(circulationClient, ecsTlrClient);
  }

  @Test
  void shouldFallbackToCirculationClientWhenNotCentralOrSecure() {
    when(settingsService.isEcsTlrFeatureEnabled(tenantId)).thenReturn(true);
    when(tenantService.isCentralTenant(tenantId)).thenReturn(false);
    when(tenantService.isSecureTenant(tenantId)).thenReturn(false);
    when(circulationClient.declareClaimedReturnedItemAsMissing(loanId, request)).thenReturn(response);

    service.declareClaimedReturnedItemAsMissing(loanId, request);

    verify(circulationClient).declareClaimedReturnedItemAsMissing(loanId, request);
    verifyNoMoreInteractions(ecsTlrClient, requestMediatedClient);
  }

  @Test
  void shouldThrowValidationExceptionWhenCirculationClientThrowsException() {
    when(settingsService.isEcsTlrFeatureEnabled(tenantId)).thenReturn(true);
    doThrow(new RuntimeException("Error")).when(circulationClient)
      .declareClaimedReturnedItemAsMissing(loanId, request);

    assertThrows(ValidationException.class,
      () -> service.declareClaimedReturnedItemAsMissing(loanId, request));
  }

}
