package org.folio.circulationbff.controller;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.InstanceSearchResult;
import org.folio.circulationbff.rest.resource.CirculationBffApi;
import org.folio.circulationbff.service.CirculationBffService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@AllArgsConstructor
public class CirculationBffController implements CirculationBffApi {

  private final CirculationBffService circulationBffService;

  @Override
  public ResponseEntity<AllowedServicePoints> circulationBffRequestsAllowedServicePointsGet(UUID patronGroupId, String operation, UUID instanceId, UUID requestId) {
    log.info("circulationBffRequestsAllowedServicePointsGet:: params: patronGroupId={}, operation={}, instanceId={}, requestId={}", patronGroupId, operation, instanceId, requestId);
    return ResponseEntity.status(HttpStatus.OK).body(circulationBffService.getAllowedServicePoints(patronGroupId, operation, instanceId, requestId));
  }

  @Override
  public ResponseEntity<InstanceSearchResult> circulationBffRequestsSearchInstancesGet(String query) {
    return ResponseEntity.status(HttpStatus.OK).body(new InstanceSearchResult());
  }
}
