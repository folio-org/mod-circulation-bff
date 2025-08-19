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
import org.springframework.http.ResponseEntity;
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
  public ResponseEntity<Void> declareClaimedReturnedItemAsMissing(UUID loanId,
    DeclareClaimedReturnedItemAsMissingRequest declareClaimedReturnedItemAsMissingRequest) {

    return perform(loanId, declareClaimedReturnedItemAsMissingRequest);
  }

  @Override
  ResponseEntity<Void> performInCirculation(UUID loanId,
    DeclareClaimedReturnedItemAsMissingRequest request) {

    return circulationClient.declareClaimedReturnedItemAsMissing(loanId, request);
  }

  @Override
  ResponseEntity<Void> performInTlr(UUID loanId,
    DeclareClaimedReturnedItemAsMissingRequest request) {

    return ecsTlrClient.declareClaimedReturnedItemAsMissing(
      new TlrDeclareClaimedReturnedItemAsMissingRequest()
        .loanId(loanId)
        .comment(request.getComment()));
  }

  @Override
  ResponseEntity<Void> performInRequestsMediated(UUID loanId,
    DeclareClaimedReturnedItemAsMissingRequest request) {

    return requestMediatedClient.declareClaimedReturnedItemAsMissing(loanId, request);
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
