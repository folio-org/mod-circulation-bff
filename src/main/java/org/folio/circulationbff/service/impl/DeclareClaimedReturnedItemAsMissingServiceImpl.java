package org.folio.circulationbff.service.impl;

import java.util.UUID;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.client.feign.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.DeclareClaimedReturnedItemAsMissingRequest;
import org.folio.circulationbff.domain.dto.TlrDeclareClaimedReturnedItemAsMissingRequest;
import org.folio.circulationbff.service.DeclareClaimedReturnedItemAsMissingService;
import org.folio.circulationbff.service.SettingsService;
import org.folio.circulationbff.service.TenantService;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class DeclareClaimedReturnedItemAsMissingServiceImpl
  extends AbstractLoanActionService<DeclareClaimedReturnedItemAsMissingRequest>
  implements DeclareClaimedReturnedItemAsMissingService {

  private final EcsTlrClient ecsTlrClient;
  private final CirculationClient circulationClient;
  private final RequestMediatedClient requestMediatedClient;

  public DeclareClaimedReturnedItemAsMissingServiceImpl(SettingsService settingsService, TenantService tenantService,
    EcsTlrClient ecsTlrClient, CirculationClient circulationClient,
    RequestMediatedClient requestMediatedClient) {

    super(settingsService, tenantService);
    this.ecsTlrClient = ecsTlrClient;
    this.circulationClient = circulationClient;
    this.requestMediatedClient = requestMediatedClient;
  }

  @Override
  public void declareClaimedReturnedItemAsMissing(UUID loanId,
    DeclareClaimedReturnedItemAsMissingRequest declareClaimedReturnedItemAsMissingRequest) {

    perform(loanId, declareClaimedReturnedItemAsMissingRequest);
  }

  @Override
  void performInCirculation(UUID loanId,
    DeclareClaimedReturnedItemAsMissingRequest request) {

    circulationClient.declareClaimedReturnedItemAsMissing(loanId, request);
  }

  @Override
  void performInTlr(UUID loanId,
    DeclareClaimedReturnedItemAsMissingRequest request) {

    ecsTlrClient.declareClaimedReturnedItemAsMissing(
      new TlrDeclareClaimedReturnedItemAsMissingRequest()
        .loanId(loanId)
        .comment(request.getComment()));
  }

  @Override
  void performInRequestsMediated(UUID loanId,
    DeclareClaimedReturnedItemAsMissingRequest request) {

    requestMediatedClient.declareClaimedReturnedItemAsMissing(loanId, request);
  }

  @Override
  String getActionName() {
    return "DeclareClaimedReturnedItemAsMissing";
  }

  @Override
  String toLogString(DeclareClaimedReturnedItemAsMissingRequest request) {
    return "DeclareClaimedReturnedItemAsMissingRequest()";
  }

}
