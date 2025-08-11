package org.folio.circulationbff.service;

import static org.folio.circulationbff.util.CirculationLoanTestData.ITEM_ID;
import static org.folio.circulationbff.util.CirculationLoanTestData.LOAN_ID;
import static org.folio.circulationbff.util.CirculationLoanTestData.USER_ID;
import static org.folio.circulationbff.util.CirculationLoanTestData.bffSearchInstance;
import static org.folio.circulationbff.util.CirculationLoanTestData.bffSearchItem;
import static org.folio.circulationbff.util.CirculationLoanTestData.circulationLoan;
import static org.folio.circulationbff.util.CirculationLoanTestData.dcbLoanItem;
import static org.folio.circulationbff.util.CirculationLoanTestData.enrichedLoanItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.domain.dto.CirculationLoans;
import org.folio.circulationbff.domain.mapping.CirculationLoanMapper;
import org.folio.circulationbff.domain.mapping.CirculationLoanMapperImpl;
import org.folio.circulationbff.service.impl.CirculationLoanServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CirculationLoanServiceTest {

  @InjectMocks private CirculationLoanServiceImpl circulationLoanService;

  @Mock private TenantService tenantService;
  @Mock private SearchService searchService;
  @Mock private SettingsService settingsService;
  @Mock private CirculationClient circulationClient;
  @Spy private CirculationLoanMapper circulationLoanMapper = new CirculationLoanMapperImpl();

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(tenantService, searchService, settingsService, circulationClient);
  }

  @Test
  void findCirculationLoansForItem() {
    var query = "(userId==" + USER_ID + ")";
    var foundLoans = new CirculationLoans()
      .loans(List.of(circulationLoan(false, enrichedLoanItem())))
      .totalRecords(1);

    when(circulationClient.findLoansByQuery(query, 200, 0, "auto")).thenReturn(foundLoans);
    var result = circulationLoanService.findCirculationLoans(query, 200, 0, "auto");

    var expectedResult = new CirculationLoans()
      .loans(List.of(circulationLoan(false, enrichedLoanItem())))
      .totalRecords(1);

    assertEquals(expectedResult, result);
    verifyNoInteractions(circulationLoanMapper);
  }

  @Test
  void findCirculationLoansForDcbItem() {
    var query = "(userId==" + USER_ID + ")";
    var nonDcbItem = circulationLoan(false, enrichedLoanItem());
    var foundLoans = new CirculationLoans()
      .loans(List.of(nonDcbItem, circulationLoan(true, dcbLoanItem())))
      .totalRecords(1);

    when(tenantService.isCurrentTenantCentral()).thenReturn(true);
    when(settingsService.isEcsTlrFeatureEnabled()).thenReturn(true);
    when(circulationClient.findLoansByQuery(query, 200, 0, "auto")).thenReturn(foundLoans);

    var expectedItemsCql = String.format("item.id==(\"%s\")", ITEM_ID);
    var bffSearchInstance = bffSearchInstance(bffSearchItem());
    when(searchService.findInstances(expectedItemsCql)).thenReturn(List.of(bffSearchInstance));
    var result = circulationLoanService.findCirculationLoans(query, 200, 0, "auto");

    var expectedResult = new CirculationLoans()
      .loans(List.of(nonDcbItem, circulationLoan(true, enrichedLoanItem())))
      .totalRecords(1);
    assertEquals(expectedResult, result);
  }

  @Test
  void findCirculationLoansForDcbItemForNonCentralSecureTenant() {
    var query = "(userId==" + USER_ID + ")";
    var nonDcbItem = circulationLoan(false, enrichedLoanItem());
    var foundLoans = new CirculationLoans()
      .loans(List.of(nonDcbItem, circulationLoan(true, dcbLoanItem())))
      .totalRecords(1);

    when(settingsService.isEcsTlrFeatureEnabled()).thenReturn(true);
    when(tenantService.isCurrentTenantCentral()).thenReturn(false);
    when(tenantService.isCurrentTenantSecure()).thenReturn(true);
    when(circulationClient.findLoansByQuery(query, 200, 0, "auto")).thenReturn(foundLoans);

    var expectedItemsCql = String.format("item.id==(\"%s\")", ITEM_ID);
    var bffSearchInstance = bffSearchInstance(bffSearchItem());
    when(searchService.findInstances(expectedItemsCql)).thenReturn(List.of(bffSearchInstance));
    var result = circulationLoanService.findCirculationLoans(query, 200, 0, "auto");

    var expectedResult = new CirculationLoans()
      .loans(List.of(nonDcbItem, circulationLoan(true, enrichedLoanItem())))
      .totalRecords(1);
    assertEquals(expectedResult, result);
  }

  @Test
  void findCirculationLoansWhenLoanItemIsNull() {
    var query = "(userId==" + USER_ID + ")";
    var foundLoans = new CirculationLoans()
      .loans(List.of(circulationLoan(false, null)))
      .totalRecords(1);

    when(circulationClient.findLoansByQuery(query, 200, 0, "auto")).thenReturn(foundLoans);

    var result = circulationLoanService.findCirculationLoans(query, 200, 0, "auto");

    var expectedResult = new CirculationLoans()
      .loans(List.of(circulationLoan(false, null)))
      .totalRecords(1);

    assertEquals(expectedResult, result);
    verifyNoInteractions(circulationLoanMapper);
  }

  @Test
  void findCirculationLoansWhenDcbLoanItemIsNull() {
    var query = "(userId==" + USER_ID + ")";
    var foundLoans = new CirculationLoans()
      .loans(List.of(circulationLoan(true, null)))
      .totalRecords(1);

    when(circulationClient.findLoansByQuery(query, 200, 0, "auto")).thenReturn(foundLoans);

    var result = circulationLoanService.findCirculationLoans(query, 200, 0, "auto");

    var expectedResult = new CirculationLoans()
      .loans(List.of(circulationLoan(true, null)))
      .totalRecords(1);

    assertEquals(expectedResult, result);
    verifyNoInteractions(circulationLoanMapper);
  }

  @Test
  void findCirculationLoansForDcbItemWhenItemNotFound() {
    var query = "(userId==" + USER_ID + ")";
    var foundLoans = new CirculationLoans()
      .loans(List.of(circulationLoan(true, dcbLoanItem())))
      .totalRecords(1);
    var expectedItemsCql = String.format("item.id==(\"%s\")", ITEM_ID);
    var bffSearchInstance = bffSearchInstance();

    when(tenantService.isCurrentTenantCentral()).thenReturn(true);
    when(settingsService.isEcsTlrFeatureEnabled()).thenReturn(true);
    when(circulationClient.findLoansByQuery(query, 200, 0, "auto")).thenReturn(foundLoans);
    when(searchService.findInstances(expectedItemsCql)).thenReturn(List.of(bffSearchInstance));

    var result = circulationLoanService.findCirculationLoans(query, 200, 0, "auto");

    var expectedResult = new CirculationLoans()
      .loans(List.of(circulationLoan(true, dcbLoanItem())))
      .totalRecords(1);
    assertEquals(expectedResult, result);
  }

  @Test
  void findCirculationLoansForDcbItemWhenTenantIsNotCentralAndNotSecure() {
    var query = "(userId==" + USER_ID + ")";
    var foundLoans = new CirculationLoans()
      .loans(List.of(circulationLoan(true, dcbLoanItem())))
      .totalRecords(1);

    when(tenantService.isCurrentTenantCentral()).thenReturn(false);
    when(settingsService.isEcsTlrFeatureEnabled()).thenReturn(true);
    when(tenantService.isCurrentTenantSecure()).thenReturn(false);
    when(circulationClient.findLoansByQuery(query, 200, 0, "auto")).thenReturn(foundLoans);

    var result = circulationLoanService.findCirculationLoans(query, 200, 0, "auto");

    var expectedResult = new CirculationLoans()
      .loans(List.of(circulationLoan(true, dcbLoanItem())))
      .totalRecords(1);

    assertEquals(expectedResult, result);
    verifyNoInteractions(circulationLoanMapper);
  }

  @Test
  void findCirculationLoansForDcbItemWhenTlrDisable() {
    var query = "(userId==" + USER_ID + ")";
    var foundLoans = new CirculationLoans()
      .loans(List.of(circulationLoan(true, dcbLoanItem())))
      .totalRecords(1);

    when(settingsService.isEcsTlrFeatureEnabled()).thenReturn(false);
    when(circulationClient.findLoansByQuery(query, 200, 0, "auto")).thenReturn(foundLoans);

    var result = circulationLoanService.findCirculationLoans(query, 200, 0, "auto");

    var expectedResult = new CirculationLoans()
      .loans(List.of(circulationLoan(true, dcbLoanItem())))
      .totalRecords(1);

    assertEquals(expectedResult, result);
    verifyNoInteractions(circulationLoanMapper, tenantService);
  }

  @Test
  void getCirculationLoanByIdForItem() {
    var loanId = UUID.fromString(LOAN_ID);
    var circulationLoan = circulationLoan(false, enrichedLoanItem());
    when(circulationClient.findLoanById(loanId)).thenReturn(circulationLoan);

    var result = circulationLoanService.getCirculationLoanById(loanId);
    assertEquals(circulationLoan, result);
    verifyNoInteractions(tenantService, settingsService, searchService);
  }

  @Test
  void getCirculationLoanByIdForDcbItem() {
    var loanId = UUID.fromString(LOAN_ID);
    var circulationLoan = circulationLoan(true, dcbLoanItem());
    var query = "item.id==(\"" + ITEM_ID + "\")";

    when(tenantService.isCurrentTenantCentral()).thenReturn(true);
    when(settingsService.isEcsTlrFeatureEnabled()).thenReturn(true);
    when(searchService.findInstances(query)).thenReturn(List.of(bffSearchInstance(bffSearchItem())));
    when(circulationClient.findLoanById(loanId)).thenReturn(circulationLoan);

    var result = circulationLoanService.getCirculationLoanById(loanId);
    assertEquals(circulationLoan(true, enrichedLoanItem()), result);
  }

  @Test
  void getCirculationLoanByIdForDcbItemWhenNonCentralSecureTenant() {
    var loanId = UUID.fromString(LOAN_ID);
    var circulationLoan = circulationLoan(true, dcbLoanItem());

    when(settingsService.isEcsTlrFeatureEnabled()).thenReturn(true);
    when(tenantService.isCurrentTenantSecure()).thenReturn(true);
    when(tenantService.isCurrentTenantCentral()).thenReturn(false);

    var query = "item.id==(\"" + ITEM_ID + "\")";
    when(searchService.findInstances(query)).thenReturn(List.of(bffSearchInstance(bffSearchItem())));
    when(circulationClient.findLoanById(loanId)).thenReturn(circulationLoan);

    var result = circulationLoanService.getCirculationLoanById(loanId);
    assertEquals(circulationLoan(true, enrichedLoanItem()), result);
  }

  @Test
  void getCirculationLoanByIdForDcbItemWhenItemNotFound() {
    var loanId = UUID.fromString(LOAN_ID);
    var circulationLoan = circulationLoan(true, dcbLoanItem());
    var query = "item.id==(\"" + ITEM_ID + "\")";

    when(tenantService.isCurrentTenantCentral()).thenReturn(true);
    when(settingsService.isEcsTlrFeatureEnabled()).thenReturn(true);
    when(searchService.findInstances(query)).thenReturn(List.of(bffSearchInstance()));
    when(circulationClient.findLoanById(loanId)).thenReturn(circulationLoan);

    var result = circulationLoanService.getCirculationLoanById(loanId);
    assertEquals(circulationLoan(true, dcbLoanItem()), result);
  }

  @Test
  void getCirculationLoanByIdForDcbItemWhenTlrIsDisabled() {
    var loanId = UUID.fromString(LOAN_ID);
    var circulationLoan = circulationLoan(true, dcbLoanItem());

    when(settingsService.isEcsTlrFeatureEnabled()).thenReturn(false);
    when(circulationClient.findLoanById(loanId)).thenReturn(circulationLoan);

    var result = circulationLoanService.getCirculationLoanById(loanId);
    assertEquals(circulationLoan(true, dcbLoanItem()), result);
    verifyNoInteractions(tenantService, searchService);
  }

  @Test
  void getCirculationLoanByIdForDcbItemWhenNotCentralNonSecureTenant() {
    var loanId = UUID.fromString(LOAN_ID);
    var circulationLoan = circulationLoan(true, dcbLoanItem());

    when(settingsService.isEcsTlrFeatureEnabled()).thenReturn(true);
    when(tenantService.isCurrentTenantSecure()).thenReturn(false);
    when(tenantService.isCurrentTenantCentral()).thenReturn(false);
    when(circulationClient.findLoanById(loanId)).thenReturn(circulationLoan);

    var result = circulationLoanService.getCirculationLoanById(loanId);
    assertEquals(circulationLoan(true, dcbLoanItem()), result);
    verifyNoInteractions(searchService);
  }

  @Test
  void getCirculationLoanByIdForNullDcbItem() {
    var loanId = UUID.fromString(LOAN_ID);
    var circulationLoan = circulationLoan(true, null);
    when(circulationClient.findLoanById(loanId)).thenReturn(circulationLoan);

    var result = circulationLoanService.getCirculationLoanById(loanId);
    assertEquals(circulationLoan(true, null), result);
    verifyNoInteractions(tenantService, settingsService, searchService);
  }
}
