package org.folio.circulationbff.controller;

import static org.springframework.http.HttpStatus.CREATED;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.AllowedServicePointParams;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.InstanceSearchResult;
import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.folio.circulationbff.rest.resource.CirculationBffApi;
import org.folio.circulationbff.service.CirculationBffService;
import org.folio.circulationbff.service.MediatedRequestsService;
import org.folio.circulationbff.service.SearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        .requesterId(requestId)
        .itemId(itemId)
        .build(),
      tenantId));
  }

  @Override
  public ResponseEntity<InstanceSearchResult> circulationBffRequestsSearchInstancesGet(String query) {
    return ResponseEntity.status(HttpStatus.OK)
      .body(searchService.findInstances(query));
  }

  @Override
  public ResponseEntity<MediatedRequest> saveAndConfirmMediatedRequest(MediatedRequest mediatedRequest) {
    log.info("postMediatedRequest:: parameters mediatedRequest: {}", mediatedRequest);

    var responseEntity = mediatedRequestsService.updateAndConfirmMediatedRequest(mediatedRequest);
    if (responseEntity.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
      log.info("saveAndConfirmMediatedRequest:: mediated request: {} has been confirmed",
        mediatedRequest.getId());

      return ResponseEntity.status(CREATED).body(mediatedRequest);
    }

    return ResponseEntity.status(responseEntity.getStatusCode()).build();
  }
}
