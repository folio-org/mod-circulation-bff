package org.folio.circulationbff.service.impl;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import com.google.common.collect.Lists;
import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.client.feign.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.AllowedServicePointParams;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.BffRequest;
import org.folio.circulationbff.domain.dto.EcsTlr;
import org.folio.circulationbff.domain.dto.PickSlipCollection;
import org.folio.circulationbff.domain.dto.Request;
import org.folio.circulationbff.domain.dto.RequestBatchRequestInfo;
import org.folio.circulationbff.domain.dto.Requests;
import org.folio.circulationbff.domain.dto.SearchSlipCollection;
import org.folio.circulationbff.service.CirculationBffService;
import org.folio.circulationbff.service.SettingsService;
import org.folio.circulationbff.service.TenantService;
import org.folio.circulationbff.service.UserService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class CirculationBffServiceImpl implements CirculationBffService {

  private final CirculationClient circulationClient;
  private final EcsTlrClient ecsTlrClient;
  private final UserService userService;
  private final SettingsService settingsService;
  private final TenantService tenantService;
  private final SystemUserScopedExecutionService executionService;
  private final RequestMediatedClient requestMediatedClient;

  @Value("${BATCH_REQUEST_DETAILS_QUERY_IDS_COUNT:20}")
  private Integer batchRequestDetailsQueryIdsSize;

  @Override
  public PickSlipCollection fetchPickSlipsByServicePointId(String servicePointId) {
    log.info("fetchPickSlipsByServicePointId:: servicePointId: {}", servicePointId);
    return shouldFetchStaffSlipsFromTlr()
      ? ecsTlrClient.getPickSlips(servicePointId)
      : circulationClient.getPickSlips(servicePointId);
  }

  @Override
  public SearchSlipCollection fetchSearchSlipsByServicePointId(String servicePointId) {
    log.info("fetchSearchSlipsByServicePointId:: servicePointId: {}", servicePointId);
    return shouldFetchStaffSlipsFromTlr()
      ? ecsTlrClient.getSearchSlips(servicePointId)
      : circulationClient.getSearchSlips(servicePointId);
  }

  @Override
  public Requests getBatchRequestInfoEnrichedRequests(String query, Integer offset, Integer limit, String totalRecords) {
    log.info("getBatchRequestInfoEnrichedRequests:: query: {}, offset: {}, limit: {}, totalRecords: {}",
      query, offset, limit, totalRecords);
    var requests = circulationClient.getRequests(query, limit, offset, totalRecords);
    var idsToRequest = requests.getRequests().stream()
      .filter(request -> Objects.nonNull(request.getId()))
      .map(request -> Map.entry(request.getId(), request))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    var batchRequestDetails = Lists.partition(new ArrayList<>(idsToRequest.keySet()), batchRequestDetailsQueryIdsSize).stream()
      .map(partition -> partition.stream().collect(Collectors.joining(" or id=", "id=", EMPTY)))
      .map(idsQuery -> requestMediatedClient.queryMediatedBatchRequestDetails(idsQuery).getMediatedBatchRequestDetails())
      .flatMap(List::stream)
      .toList();

    Map<String, Date> batchIdsToSubmittedAtDates = new HashMap<>();
    for (var batchDetail : batchRequestDetails) {
      var request = idsToRequest.get(batchDetail.getConfirmedRequestId());
      if (request != null) {
        var batchId = batchDetail.getBatchId();
        var submittedAt = batchIdsToSubmittedAtDates.computeIfAbsent(batchId, this::getBatchSubmittedAtDate);
        request.setBatchRequestInfo(new RequestBatchRequestInfo(batchId, submittedAt));
      }
    }

    return requests;
  }

  private Date getBatchSubmittedAtDate(String batchId) {
    var batchRequestResponse = requestMediatedClient.getMediatedBatchRequestById(UUID.fromString(batchId));

    if (!batchRequestResponse.getStatusCode().equals(HttpStatus.OK) || batchRequestResponse.getBody() == null) {
      log.warn("getBatchSubmittedAtDate:: Unable to fetch batch request with id: {}, status: {}, body: {}",
        batchId, batchRequestResponse.getStatusCode(), batchRequestResponse.getBody());
      return null;
    }

    return batchRequestResponse.getBody().getRequestDate();
  }

  @Override
  public AllowedServicePoints getAllowedServicePoints(AllowedServicePointParams params, String tenantId) {
    log.info("getAllowedServicePoints:: params: {}", params);

    if (!settingsService.isEcsTlrFeatureEnabled()) {
      log.info("getAllowedServicePoints:: Ecs TLR Feature is disabled. " +
        "Calling local mod-circulation.");
      return circulationClient.allowedServicePoints(params);
    }

    if (tenantService.isCurrentTenantCentral()) {
      log.info("getAllowedServicePoints:: Ecs TLR Feature is enabled and we are in the central " +
        "tenant. Calling local mod-tlr.");
      return ecsTlrClient.getAllowedServicePoints(params);
    }

    log.info("getAllowedServicePoints:: Ecs TLR Feature is enabled and current tenant is not " +
      "central.");

    if (params.getRequestId() == null) {
      log.info("getAllowedServicePoints:: Request ID is missing (creation). Calling central mod-tlr.");
      // This should handle both mediated request and local data tenant request cases.
      // In case of a local request, central mod-tlr should call mod-circulation of the current
      // data tenant anyway.
      return getAllowedSpFromCentralTlr(params);
    }

    log.info("getAllowedServicePoints:: Request ID is present (editing).");

    var request = circulationClient.getRequestById(params.getRequestId().toString());
    if (request.getEcsRequestPhase() == null) {
      log.info("getAllowedServicePoints:: Request is not an ECS request. Calling local mod-circulation.");
      return circulationClient.allowedServicePoints(params);
    }

    log.info("getAllowedServicePoints:: Ecs TLR Feature is enabled and request is " +
      "an ECS request. Calling central mod-tlr.");
    return getAllowedSpFromCentralTlr(params);
  }

  private AllowedServicePoints getAllowedSpFromCentralTlr(AllowedServicePointParams params) {
    String patronGroupId = params.getRequesterId() == null ? null :
      userService.find(params.getRequesterId().toString()).getPatronGroup();
    params.setPatronGroupId(patronGroupId != null ? UUID.fromString(patronGroupId) : null);
    params.setRequesterId(null);
    return executionService.executeSystemUserScoped(
      tenantService.getCentralTenantId().orElseThrow(),
      () -> ecsTlrClient.getAllowedServicePoints(params));
  }

  @Override
  public Request createRequest(BffRequest request, String tenantId) {
    log.info("createRequest:: request: {}", request.getId());
    if (settingsService.isEcsTlrFeatureEnabled(tenantId) && tenantService.isCentralTenant(tenantId)) {
      log.info("createRequest:: ECS TLR Feature is enabled. Creating ECS TLR");
      EcsTlr tlrRequest = ecsTlrClient.createRequest(request);
      return circulationClient.getRequestById(tlrRequest.getPrimaryRequestId());
    } else {
      log.info("createRequest:: Ecs TLR Feature is disabled. Creating circulation request");
      return circulationClient.createRequest(request);
    }
  }

  private boolean shouldFetchStaffSlipsFromTlr() {
    boolean isCentralTenant = tenantService.isCurrentTenantCentral();
    boolean ecsTlrFeatureIsEnabledInTlr = false;
    if (isCentralTenant) {
      ecsTlrFeatureIsEnabledInTlr = ecsTlrClient.getTlrSettings().getEcsTlrFeatureEnabled();
    }
    log.info("shouldFetchStaffSlipsFromTlr:: {}", ecsTlrFeatureIsEnabledInTlr);
    return ecsTlrFeatureIsEnabledInTlr;
  }
}
