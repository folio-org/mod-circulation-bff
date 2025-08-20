package org.folio.circulationbff.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.client.feign.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.ClaimItemReturnedRequest;
import org.folio.circulationbff.domain.dto.TlrClaimItemReturnedRequest;
import org.folio.circulationbff.domain.mapping.TlrClaimItemReturnedRequestMapper;
import org.folio.circulationbff.service.impl.ClaimItemReturnedServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

class ClaimItemReturnedServiceTest {

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
  @Mock
  private TlrClaimItemReturnedRequestMapper tlrClaimItemReturnedRequestMapper;
  @InjectMocks
  private ClaimItemReturnedServiceImpl service;

  private final UUID loanId = UUID.randomUUID();
  private final ClaimItemReturnedRequest request = new ClaimItemReturnedRequest();
  private final String tenantId = "tenant";
  private final ResponseEntity<Void> response = ResponseEntity.noContent().build();

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    service = new ClaimItemReturnedServiceImpl(settingsService, tenantService, ecsTlrClient, circulationClient, requestMediatedClient, tlrClaimItemReturnedRequestMapper);
    when(tenantService.getCurrentTenantId()).thenReturn(tenantId);
  }

  @Test
  void shouldUseCirculationClientWhenEcsTlrFeatureDisabled() {
    when(settingsService.isEcsTlrFeatureEnabled(tenantId)).thenReturn(false);
    when(circulationClient.claimItemReturned(loanId, request)).thenReturn(response);

    service.claimItemReturned(loanId, request);

    verify(circulationClient).claimItemReturned(loanId, request);
    verifyNoMoreInteractions(ecsTlrClient, requestMediatedClient);
  }

  @Test
  void shouldUseEcsTlrClientWhenCentralTenant() {
    var tlrClaimItemReturnedRequest = new TlrClaimItemReturnedRequest().loanId(loanId);
    when(settingsService.isEcsTlrFeatureEnabled(tenantId)).thenReturn(true);
    when(tenantService.isCentralTenant(tenantId)).thenReturn(true);
    when(tlrClaimItemReturnedRequestMapper.toTlrClaimItemReturnedRequest(loanId, request)).thenReturn(tlrClaimItemReturnedRequest);
    when(ecsTlrClient.claimItemReturned(tlrClaimItemReturnedRequest)).thenReturn(response);

    service.claimItemReturned(loanId, request);

    verify(ecsTlrClient).claimItemReturned(tlrClaimItemReturnedRequest);
    verifyNoMoreInteractions(circulationClient, requestMediatedClient);
  }

  @Test
  void shouldUseRequestMediatedClientWhenCurrentTenantSecure() {
    when(settingsService.isEcsTlrFeatureEnabled(tenantId)).thenReturn(true);
    when(tenantService.isCentralTenant(tenantId)).thenReturn(false);
    when(tenantService.isSecureTenant(tenantId)).thenReturn(true);
    when(requestMediatedClient.claimItemReturned(loanId, request)).thenReturn(response);

    service.claimItemReturned(loanId, request);

    verify(requestMediatedClient).claimItemReturned(loanId, request);
    verifyNoMoreInteractions(circulationClient, ecsTlrClient);
  }

  @Test
  void shouldFallbackToCirculationClientWhenNotCentralOrSecure() {
    when(settingsService.isEcsTlrFeatureEnabled(tenantId)).thenReturn(true);
    when(tenantService.isCentralTenant(tenantId)).thenReturn(false);
    when(tenantService.isSecureTenant(tenantId)).thenReturn(false);
    when(circulationClient.claimItemReturned(loanId, request)).thenReturn(response);

    service.claimItemReturned(loanId, request);

    verify(circulationClient).claimItemReturned(loanId, request);
    verifyNoMoreInteractions(ecsTlrClient, requestMediatedClient);
  }
}
