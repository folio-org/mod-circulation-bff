package org.folio.circulationbff.controller;

import org.folio.tenant.domain.dto.TenantAttributes;
import org.folio.tenant.rest.resource.TenantApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController("folioTenantController")
@RequestMapping
public class TenantController implements TenantApi {

  @Override
  public ResponseEntity<Void> deleteTenant(String operationId) {
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<String> getTenant(String operationId) {
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @Override
  public ResponseEntity<Void> postTenant(TenantAttributes tenantAttributes) {
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

}
