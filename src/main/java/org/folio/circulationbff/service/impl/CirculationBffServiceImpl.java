package org.folio.circulationbff.service.impl;

import static com.google.common.collect.Lists.partition;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.folio.circulationbff.support.CqlQuery.exactMatchAny;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.client.feign.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.AllowedServicePointParams;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.BatchRequestDetail;
import org.folio.circulationbff.domain.dto.BffRequest;
import org.folio.circulationbff.domain.dto.EcsTlr;
import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.folio.circulationbff.domain.dto.PickSlipCollection;
import org.folio.circulationbff.domain.dto.Request;
import org.folio.circulationbff.domain.dto.RequestBatchRequestInfo;
import org.folio.circulationbff.domain.dto.Requests;
import org.folio.circulationbff.domain.dto.SearchSlipCollection;
import org.folio.circulationbff.service.CirculationBffService;
import org.folio.circulationbff.service.SettingsService;
import org.folio.circulationbff.service.TenantService;
import org.folio.circulationbff.service.UserService;
import org.folio.circulationbff.support.CqlQuery;
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

  private static final String CONFIRMED_REQUEST_ID_FIELD = "confirmedRequestId";

  private final CirculationClient circulationClient;
  private final EcsTlrClient ecsTlrClient;
  private final UserService userService;
  private final SettingsService settingsService;
  private final TenantService tenantService;
  private final SystemUserScopedExecutionService executionService;
  private final RequestMediatedClient requestMediatedClient;

  @Value("${folio.batch-requests.query-request-ids-count}")
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
      .collect(Collectors.toMap(Request::getId, Function.identity()));

    var batchRequestDetailsByRequestIds = getBatchRequestDetailsByRequestIds(idsToRequest.keySet());

    Map<String, Date> batchIdsToSubmittedAtDates = new HashMap<>();
    for (var entry : batchRequestDetailsByRequestIds.entrySet()) {
      var request = idsToRequest.get(entry.getKey());
      if (request != null) {
        var batchId = entry.getValue().getBatchId();
        var submittedAt = batchIdsToSubmittedAtDates.computeIfAbsent(batchId, this::getBatchSubmittedAtDate);
        request.setBatchRequestInfo(new RequestBatchRequestInfo(batchId, submittedAt));
      }
    }

    return requests;
  }

  private Map<String, BatchRequestDetail> getBatchRequestDetailsByRequestIds(Set<String> circulationRequestIds) {
    if (isNotEmpty(circulationRequestIds) && tenantService.isCurrentTenantSecure()) {
      // finding the batch requests created by secure tenant differs from the one created by non-secure tenant,
      // as for secure tenant, confirmedRequestId field of batch request detail will have mediated request id instead of circulation request id,
      // and only confirmed mediated requests will have circulation request id in their same name confirmedRequestId field,
      // thus first we need to search for the confirmed mediated requests, and then we can query batch request details by those mediated request ids
      var statusQuery = exactMatchAny("mediatedRequestStatusText", List.of("Open", "Closed"))
        .and(new CqlQuery("requestLevelText==\"Item\""));
      var mediatedToCirculationIds = partition(new ArrayList<>(circulationRequestIds), batchRequestDetailsQueryIdsSize).stream()
        .map(ids -> exactMatchAny(CONFIRMED_REQUEST_ID_FIELD, ids).and(statusQuery))
        .map(cqlQuery -> requestMediatedClient.getMediatedRequestsByQuery(cqlQuery.query()).getMediatedRequests())
        .flatMap(List::stream)
        .filter(mediatedRequest -> isNotBlank(mediatedRequest.getConfirmedRequestId()))
        .collect(Collectors.toMap(MediatedRequest::getId, MediatedRequest::getConfirmedRequestId));

      return partition(new ArrayList<>(mediatedToCirculationIds.keySet()), batchRequestDetailsQueryIdsSize).stream()
        .map(partition -> exactMatchAny(CONFIRMED_REQUEST_ID_FIELD, partition))
        .map(cqlQuery -> requestMediatedClient.queryMediatedBatchRequestDetails(cqlQuery.query()).getMediatedBatchRequestDetails())
        .flatMap(List::stream)
        .collect(Collectors.toMap(detail -> mediatedToCirculationIds.get(detail.getConfirmedRequestId()), Function.identity()));
    }

    return partition(new ArrayList<>(circulationRequestIds), batchRequestDetailsQueryIdsSize).stream()
      .map(partition -> exactMatchAny(CONFIRMED_REQUEST_ID_FIELD, partition))
      .map(cqlQuery -> requestMediatedClient.queryMediatedBatchRequestDetails(cqlQuery.query()).getMediatedBatchRequestDetails())
      .flatMap(List::stream)
      .collect(Collectors.toMap(BatchRequestDetail::getConfirmedRequestId, Function.identity()));
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
