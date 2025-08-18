package org.folio.circulationbff.service.impl;

import static java.lang.String.format;

import java.util.UUID;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.client.feign.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.DeclareItemLostRequest;
import org.folio.circulationbff.domain.dto.TlrDeclareItemLostRequest;
import org.folio.circulationbff.service.DeclareItemLostService;
import org.folio.circulationbff.service.SettingsService;
import org.folio.circulationbff.service.TenantService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class DeclareItemLostServiceImpl extends LoanActionServiceImpl<DeclareItemLostRequest>
  implements DeclareItemLostService {

  private final EcsTlrClient ecsTlrClient;
  private final CirculationClient circulationClient;
  private final RequestMediatedClient requestMediatedClient;

  public DeclareItemLostServiceImpl(SettingsService settingsService, TenantService tenantService,
    EcsTlrClient ecsTlrClient, CirculationClient circulationClient,
    RequestMediatedClient requestMediatedClient) {

    super(settingsService, tenantService);
    this.ecsTlrClient = ecsTlrClient;
    this.circulationClient = circulationClient;
    this.requestMediatedClient = requestMediatedClient;
  }

  @Override
  public ResponseEntity<Void> declareItemLost(UUID loanId,
    DeclareItemLostRequest declareItemLostRequest) {

    return perform(loanId, declareItemLostRequest);
  }

  @Override
  public ResponseEntity<Void> performInCirculation(UUID loanId, DeclareItemLostRequest request) {
    return circulationClient.declareItemLost(loanId, request);
  }

  @Override
  public ResponseEntity<Void> performInTlr(UUID loanId, DeclareItemLostRequest request) {
    return ecsTlrClient.declareItemLost(new TlrDeclareItemLostRequest()
      .declaredLostDateTime(request.getDeclaredLostDateTime())
      .comment(request.getComment())
      .servicePointId(request.getServicePointId())
      .loanId(loanId));
  }

  @Override
  public ResponseEntity<Void> performInRequestsMediated(UUID loanId,
    DeclareItemLostRequest request) {

    return requestMediatedClient.declareItemLost(loanId, request);
  }

  @Override
  public String getActionName() {
    return "declareItemLost";
  }

  @Override
  public String toLogString(DeclareItemLostRequest request) {
    return format("DeclareItemLostRequest(declaredLostDateTime=%s, servicePointId=%s)",
      request.getDeclaredLostDateTime(), request.getServicePointId());
  }

}
