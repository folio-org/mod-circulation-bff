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
import org.springframework.stereotype.Service;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class DeclareItemLostServiceImpl extends AbstractLoanActionService<DeclareItemLostRequest>
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
  public void declareItemLost(UUID loanId,
    DeclareItemLostRequest declareItemLostRequest) {

    perform(loanId, declareItemLostRequest);
  }

  @Override
  public void performInCirculation(UUID loanId, DeclareItemLostRequest request) {
    circulationClient.declareItemLost(loanId, request);
  }

  @Override
  public void performInTlr(UUID loanId, DeclareItemLostRequest request) {
    ecsTlrClient.declareItemLost(new TlrDeclareItemLostRequest()
      .declaredLostDateTime(request.getDeclaredLostDateTime())
      .comment(request.getComment())
      .servicePointId(request.getServicePointId())
      .loanId(loanId));
  }

  @Override
  public void performInRequestsMediated(UUID loanId,
    DeclareItemLostRequest request) {

    requestMediatedClient.declareItemLost(loanId, request);
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
