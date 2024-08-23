package org.folio.circulationbff.controller;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.AllowedServicePointsResponse;
import org.folio.circulationbff.rest.resource.CirculationBffApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@AllArgsConstructor
public class CirculationBffController implements CirculationBffApi {
  @Override
  public ResponseEntity<AllowedServicePointsResponse> getAllowedServicePoints(String operation,
    UUID patronGroupId, UUID instanceId, UUID requestId) {

    return ResponseEntity.status(HttpStatus.OK).body(new AllowedServicePointsResponse());
  }
}