package org.folio.circulationbff.service.impl;

import static java.lang.String.format;

import java.util.UUID;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.client.feign.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.ClaimItemReturnedRequest;
import org.folio.circulationbff.domain.mapping.TlrClaimItemReturnedRequestMapper;
import org.folio.circulationbff.service.ClaimItemReturnedService;
import org.folio.circulationbff.service.SettingsService;
import org.folio.circulationbff.service.TenantService;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class ClaimItemReturnedServiceImpl extends AbstractLoanActionService<ClaimItemReturnedRequest>
  implements ClaimItemReturnedService {

  private final EcsTlrClient ecsTlrClient;
  private final CirculationClient circulationClient;
  private final RequestMediatedClient requestMediatedClient;
  private final TlrClaimItemReturnedRequestMapper tlrClaimItemReturnedRequestMapper;

  public ClaimItemReturnedServiceImpl(SettingsService settingsService, TenantService tenantService,
    EcsTlrClient ecsTlrClient, CirculationClient circulationClient,
    RequestMediatedClient requestMediatedClient,
    TlrClaimItemReturnedRequestMapper tlrClaimItemReturnedRequestMapper) {

    super(settingsService, tenantService);
    this.ecsTlrClient = ecsTlrClient;
    this.circulationClient = circulationClient;
    this.requestMediatedClient = requestMediatedClient;
    this.tlrClaimItemReturnedRequestMapper = tlrClaimItemReturnedRequestMapper;
  }

  @Override
  public void claimItemReturned(UUID loanId,
    ClaimItemReturnedRequest claimItemReturnedRequest) {

    perform(loanId, claimItemReturnedRequest);
  }

  @Override
  public void performInCirculation(UUID loanId, ClaimItemReturnedRequest request) {
    circulationClient.claimItemReturned(loanId, request);
  }

  @Override
  public void performInTlr(UUID loanId, ClaimItemReturnedRequest request) {
    ecsTlrClient.claimItemReturned(
      tlrClaimItemReturnedRequestMapper.toTlrClaimItemReturnedRequest(loanId, request));
  }

  @Override
  public void performInRequestsMediated(UUID loanId,
    ClaimItemReturnedRequest request) {

    requestMediatedClient.claimItemReturned(loanId, request);
  }

  @Override
  public String getActionName() {
    return "ClaimItemReturned";
  }

  @Override
  public String toLogString(ClaimItemReturnedRequest request) {
    return format("ClaimItemReturnedRequest(itemClaimedReturnedDateTime=%s)",
      request.getItemClaimedReturnedDateTime());
  }
}
