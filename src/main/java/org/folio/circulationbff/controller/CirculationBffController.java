package org.folio.circulationbff.controller;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.InstanceSearchResult;
import org.folio.circulationbff.rest.resource.CirculationBffApi;
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

  private final SearchService searchService;

  @Override
  public ResponseEntity<AllowedServicePoints> circulationBffRequestsAllowedServicePointsGet(
    UUID patronGroupId, String operation, UUID instanceId, UUID requestId) {

    return ResponseEntity.status(HttpStatus.OK).body(new AllowedServicePoints());
  }

  @Override
  public ResponseEntity<InstanceSearchResult> circulationBffRequestsSearchInstancesGet(String query) {
    return ResponseEntity.status(HttpStatus.OK)
      .body(searchService.findInstances(query));
  }
}