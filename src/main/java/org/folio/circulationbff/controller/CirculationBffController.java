package org.folio.circulationbff.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.Collection;
import java.util.UUID;

import org.folio.circulationbff.domain.dto.AllowedServicePointParams;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.BffRequest;
import org.folio.circulationbff.domain.dto.BffSearchInstance;
import org.folio.circulationbff.domain.dto.CheckInRequest;
import org.folio.circulationbff.domain.dto.CheckInResponse;
import org.folio.circulationbff.domain.dto.CheckOutRequest;
import org.folio.circulationbff.domain.dto.CheckOutResponse;
import org.folio.circulationbff.domain.dto.CirculationLoan;
import org.folio.circulationbff.domain.dto.CirculationLoans;
import org.folio.circulationbff.domain.dto.DeclareItemLostRequest;
import org.folio.circulationbff.domain.dto.EcsRequestExternal;
import org.folio.circulationbff.domain.dto.EmptyBffSearchInstance;
import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.folio.circulationbff.domain.dto.PickSlipCollection;
import org.folio.circulationbff.domain.dto.PostEcsRequestExternal201Response;
import org.folio.circulationbff.domain.dto.Request;
import org.folio.circulationbff.domain.dto.SearchSlipCollection;
import org.folio.circulationbff.domain.dto.UserCollection;
import org.folio.circulationbff.exception.HttpFailureFeignException;
import org.folio.circulationbff.rest.resource.CirculationBffApi;
import org.folio.circulationbff.service.CheckInService;
import org.folio.circulationbff.service.CheckOutService;
import org.folio.circulationbff.service.CirculationBffService;
import org.folio.circulationbff.service.CirculationLoanService;
import org.folio.circulationbff.service.DeclareItemLostService;
import org.folio.circulationbff.service.EcsRequestExternalService;
import org.folio.circulationbff.service.MediatedRequestsService;
import org.folio.circulationbff.service.SearchService;
import org.folio.circulationbff.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@RequiredArgsConstructor
public class CirculationBffController implements CirculationBffApi {

  private final CirculationBffService circulationBffService;
  private final SearchService searchService;
  private final MediatedRequestsService mediatedRequestsService;
  private final UserService userService;
  private final EcsRequestExternalService ecsRequestExternalService;
  private final CheckInService checkInService;
  private final CheckOutService checkOutService;
  private final CirculationLoanService circulationLoanService;
  private final DeclareItemLostService declareItemLostService;

  @Override
  public ResponseEntity<PostEcsRequestExternal201Response> postEcsRequestExternal(
    EcsRequestExternal ecsRequestExternal) {

    Object createdRequest = ecsRequestExternalService.createEcsRequestExternal(ecsRequestExternal);
    return ResponseEntity.status(CREATED)
      .body((PostEcsRequestExternal201Response) createdRequest);
  }

  @Override
  public ResponseEntity<PickSlipCollection> getPickSlips(String servicePointId) {
    log.info("getPickSlips:: servicePointId = {}", servicePointId);

    return ResponseEntity.status(HttpStatus.OK)
      .body(circulationBffService.fetchPickSlipsByServicePointId(servicePointId));
  }

  @Override
  public ResponseEntity<SearchSlipCollection> getSearchSlips(String servicePointId) {
    log.info("getSearchSlips:: servicePointId = {}", servicePointId);

    return ResponseEntity.status(HttpStatus.OK).body(circulationBffService
      .fetchSearchSlipsByServicePointId(servicePointId));
  }

  @Override
  public ResponseEntity<UserCollection> getExternalUsers(String externalUserId, String tenantId) {
    log.info("getExternalUser:: externalUserId = {}, tenantId = {}", externalUserId,
      tenantId);

    return ResponseEntity.status(HttpStatus.OK)
      .body(userService.getExternalUser(externalUserId, tenantId));
  }

  @Override
  public ResponseEntity<AllowedServicePoints> circulationBffRequestsAllowedServicePointsGet(
    String operation, String tenantId, UUID patronGroupId, UUID instanceId, UUID requestId,
    UUID requesterId, UUID itemId) {

    log.info("circulationBffRequestsAllowedServicePointsGet:: params: " +
        "patronGroupId={}, operation={}, instanceId={}, requestId={}, requesterId={}, itemId={}",
      patronGroupId, operation, instanceId, requestId, requesterId, itemId);

    return ResponseEntity.status(HttpStatus.OK).body(circulationBffService.getAllowedServicePoints(
      AllowedServicePointParams.builder()
        .operation(operation)
        .patronGroupId(patronGroupId)
        .instanceId(instanceId)
        .requestId(requestId)
        .requesterId(requesterId)
        .itemId(itemId)
        .build(),
      tenantId));
  }

  @Override
  public ResponseEntity<BffSearchInstance> circulationBffRequestsSearchInstancesGet(String query) {
    Collection<BffSearchInstance> instances = searchService.findInstances(query);

    // frontend expects either a single instance, or an empty JSON
    BffSearchInstance response = instances.stream()
      .findFirst()
      .orElseGet(EmptyBffSearchInstance::new);

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<MediatedRequest> saveAndConfirmMediatedRequest(
    MediatedRequest mediatedRequest) {

    log.info("saveAndConfirmMediatedRequest:: parameters mediatedRequest: {}", mediatedRequest);

    if (mediatedRequest.getId() != null) {
      return handleExistingRequest(mediatedRequest);
    } else {
      return handleNewRequest(mediatedRequest);
    }
  }

  @Override
  public ResponseEntity<CirculationLoans> findCirculationLoansByQuery(String query, Integer limit,
    Integer offset, String totalRecords) {
    var loans = circulationLoanService.findCirculationLoans(query, limit, offset, totalRecords);
    return ResponseEntity.ok(loans);
  }

  @Override
  public ResponseEntity<CirculationLoan> getCirculationLoanById(UUID loanId) {
    return ResponseEntity.ok(circulationLoanService.getCirculationLoanById(loanId));
  }

  private ResponseEntity<MediatedRequest> handleExistingRequest(MediatedRequest mediatedRequest) {
    log.info("handleExistingRequest:: mediatedRequest: {}", mediatedRequest.getId());

    ResponseEntity<Void> updateResponse = mediatedRequestsService.updateMediatedRequest(mediatedRequest);
    if (!updateResponse.getStatusCode().equals(NO_CONTENT)) {
      log.warn("handleExistingRequest:: the mediated request has not been updated, status: {}, " +
        "message: {}", updateResponse.getStatusCode(), updateResponse.getBody());
      return ResponseEntity.status(updateResponse.getStatusCode()).build();
    }
    log.info("handleExistingRequest:: mediated request {} has been updated",
      mediatedRequest.getId());

    return confirmMediatedRequest(mediatedRequest);
  }

  private ResponseEntity<MediatedRequest> handleNewRequest(MediatedRequest mediatedRequest) {
    log.info("handleNewRequest:: creating new mediated request");

    ResponseEntity<MediatedRequest> createResponse = mediatedRequestsService
      .saveMediatedRequest(mediatedRequest);
    var postResponseBody = createResponse.getBody();
    if (!createResponse.getStatusCode().equals(CREATED) || postResponseBody == null) {
      log.warn("handleNewRequest:: failed to create new mediated request, status: {}, " +
        "message: {}", createResponse.getStatusCode(), postResponseBody);

      return ResponseEntity.status(createResponse.getStatusCode()).build();
    }
    log.info("handleNewRequest:: mediated request has been created");

    return confirmMediatedRequest(postResponseBody);
  }

  private ResponseEntity<MediatedRequest> confirmMediatedRequest(MediatedRequest mediatedRequest) {
    log.info("confirmMediatedRequest:: confirming mediated request: {}", mediatedRequest.getId());
    ResponseEntity<Void> confirmResponse = mediatedRequestsService.confirmMediatedRequest(mediatedRequest);
    if (!confirmResponse.getStatusCode().equals(NO_CONTENT)) {
      log.info("confirmMediatedRequest:: mediated request {} has not been confirmed",
        mediatedRequest.getId());

      return ResponseEntity.status(confirmResponse.getStatusCode()).build();
    }
    log.info("confirmMediatedRequest:: mediated request has been confirmed: {}",
      mediatedRequest.getId());

    return ResponseEntity.status(CREATED).body(mediatedRequest);
  }

  @Override
  public ResponseEntity<Request> createRequest(String tenantId, BffRequest bffRequest) {
    log.info("createRequest:: tenantId: {}, requestId: {}", tenantId, bffRequest.getId());
    return ResponseEntity.status(CREATED)
      .body(circulationBffService.createRequest(bffRequest, tenantId));
  }

  @Override
  public ResponseEntity<CheckInResponse> checkInByBarcode(CheckInRequest checkInRequest) {
    log.info("checkInByBarcode:: itemBarcode: {}", checkInRequest::getItemBarcode);
    return ResponseEntity.ok(checkInService.checkIn(checkInRequest));
  }

  @Override
  public ResponseEntity<CheckOutResponse> checkOutByBarcode(CheckOutRequest checkOutRequest) {
    log.info("checkOutByBarcode:: itemBarcode: {}", checkOutRequest::getItemBarcode);
    return ResponseEntity.ok(checkOutService.checkOut(checkOutRequest));
  }

  @ExceptionHandler(HttpFailureFeignException.class)
  public ResponseEntity<String> handleFeignException(HttpFailureFeignException e) {
    log.warn("handleFeignException:: forwarding error response with status {} from {}",
      e::getStatusCode, e::getUrl);
    return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBody());
  }

  @Override
  public ResponseEntity<Void> declareItemLost(UUID loanId, DeclareItemLostRequest declareLostRequest) {
    log.info("declareItemLost:: loanId: {}, declareItemLostRequest: {}", loanId, declareLostRequest);
    return declareItemLostService.declareItemLost(loanId, declareLostRequest);
  }
}
