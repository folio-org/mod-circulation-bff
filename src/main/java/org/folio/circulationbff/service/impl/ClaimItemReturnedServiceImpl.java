package org.folio.circulationbff.service.impl;

import java.util.UUID;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.client.feign.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.ClaimItemReturnedRequest;
import org.folio.circulationbff.domain.dto.TlrClaimItemReturnedRequest;
import org.folio.circulationbff.service.ClaimItemReturnedService;
import org.folio.circulationbff.service.SettingsService;
import org.folio.circulationbff.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class ClaimItemReturnedServiceImpl implements ClaimItemReturnedService {

  private final SettingsService settingsService;
  private final TenantService tenantService;
  private final EcsTlrClient ecsTlrClient;
  private final CirculationClient circulationClient;
  private final RequestMediatedClient requestMediatedClient;

  @Override
  public ResponseEntity<Void> claimItemReturned(UUID loanId, ClaimItemReturnedRequest claimItemReturnedRequest) {
    log.info("claimItemReturned:: loanId: {}, claimItemReturnedRequest: {}", loanId, claimItemReturnedRequest);

    String currentTenantId = tenantService.getCurrentTenantId();

    if (!settingsService.isEcsTlrFeatureEnabled(currentTenantId)) {
      log.info("claimItemReturned:: ECS TLR feature is not enabled for tenant: {}, using local service", currentTenantId);
      return circulationClient.claimItemReturned(loanId, claimItemReturnedRequest);
    }

    log.info("claimItemReturned:: ECS TLR feature is enabled for tenant: {}", currentTenantId);

    if (tenantService.isCentralTenant(currentTenantId)) {
      log.info("claimItemReturned:: doing claiming item returned in central tenant");
      return ecsTlrClient.claimItemReturned(new TlrClaimItemReturnedRequest()
        .loanId(loanId)
        .itemClaimedReturnedDateTime(claimItemReturnedRequest.getItemClaimedReturnedDateTime())
        .comment(claimItemReturnedRequest.getComment()));
    }

    if (tenantService.isSecureTenant(currentTenantId)) {
      log.info("claimItemReturned:: doing claim item returned in secure tenant");
      return requestMediatedClient.claimItemReturned(loanId, claimItemReturnedRequest);
    }

    log.info("claimItemReturned:: tenant is not central or secure, using local service");
    return circulationClient.claimItemReturned(loanId, claimItemReturnedRequest);
  }
}

