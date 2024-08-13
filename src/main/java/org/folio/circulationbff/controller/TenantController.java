package org.folio.circulationbff.controller;

import org.folio.tenant.rest.resource.TenantApi;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController("folioTenantController")
@RequestMapping(value = "/_/")
public class TenantController implements TenantApi {}
