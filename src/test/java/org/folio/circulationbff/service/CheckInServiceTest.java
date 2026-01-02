package org.folio.circulationbff.service;

import static java.util.Optional.of;
import static org.folio.circulationbff.util.MockHelper.mockSystemUserService;
import static org.folio.circulationbff.util.TestUtils.randomId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.folio.circulationbff.client.feign.CheckInClient;
import org.folio.circulationbff.client.feign.CirculationItemClient;
import org.folio.circulationbff.client.feign.HoldingsStorageClient;
import org.folio.circulationbff.client.feign.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.CheckInRequest;
import org.folio.circulationbff.domain.dto.CheckInResponse;
import org.folio.circulationbff.domain.dto.CheckInResponseItem;
import org.folio.circulationbff.domain.dto.CheckInResponseLoan;
import org.folio.circulationbff.domain.dto.CheckInResponseLoanBorrower;
import org.folio.circulationbff.domain.dto.CirculationItem;
import org.folio.circulationbff.domain.dto.CirculationItemStatus;
import org.folio.circulationbff.domain.dto.Loan;
import org.folio.circulationbff.domain.dto.LoanStatus;
import org.folio.circulationbff.domain.dto.SearchInstance;
import org.folio.circulationbff.domain.dto.SearchItem;
import org.folio.circulationbff.domain.dto.User;
import org.folio.circulationbff.domain.dto.UserPersonal;
import org.folio.circulationbff.service.impl.CheckInServiceImpl;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {

  @Mock
  private CheckInClient checkInClient;
  @Mock
  private CirculationItemClient circulationItemClient;
  @Mock
  private HoldingsStorageClient holdingsStorageClient;
  @Mock
  private RequestMediatedClient requestMediatedClient;
  @Mock
  private SettingsService settingsService;
  @Mock
  private TenantService tenantService;
  @Mock
  private SearchService searchService;
  @Mock
  private InventoryService inventoryService;
  @Mock
  private CirculationStorageService circulationStorageService;
  @Mock
  private SystemUserScopedExecutionService systemUserService;
  @Mock
  private UserService userService;
  @InjectMocks
  private CheckInServiceImpl checkInService;

  @BeforeEach
  void beforeEach() {
    mockSystemUserService(systemUserService);
  }

  @Test
  void checkInIsForwardedToRequestsMediated() {
    String itemBarcode = "test_item_barcode";
    String itemId = randomId();
    String instanceId = randomId();

    when(settingsService.isEcsTlrFeatureEnabled())
      .thenReturn(true);

    when(tenantService.isCurrentTenantCentral())
      .thenReturn(true);

    SearchInstance searchInstance = new SearchInstance()
      .id(instanceId)
      .items(List.of(new SearchItem()
        .id(itemId)
        .barcode(itemBarcode)
        .tenantId("secure_tenant")));

    when(searchService.findInstanceByItemBarcode(itemBarcode))
      .thenReturn(of(searchInstance));

    when(circulationItemClient.getCirculationItem(itemId))
      .thenReturn(Optional.empty());

    when(tenantService.isSecureTenant("secure_tenant"))
      .thenReturn(true);

    when(tenantService.getSecureTenantId())
      .thenReturn(of("secure_tenant"));

    Date checkInDate = new Date();
    UUID servicePointId = UUID.randomUUID();

    CheckInRequest checkInRequest = new CheckInRequest()
      .itemBarcode(itemBarcode)
      .servicePointId(servicePointId)
      .checkInDate(checkInDate);

    CheckInResponse requestMediatedCheckInResponse = new CheckInResponse()
      .loan(new CheckInResponseLoan())
      .item(new CheckInResponseItem()
        .id(itemId)
        .instanceId(instanceId));

    when(requestMediatedClient.checkInByBarcode(checkInRequest))
      .thenReturn(requestMediatedCheckInResponse);

    CheckInResponse checkInResponse = checkInService.checkIn(checkInRequest);

    CheckInResponse expectedCheckInResponse = new CheckInResponse()
      .loan(new CheckInResponseLoan())
      .item(new CheckInResponseItem()
        .id(itemId)
        .instanceId(instanceId));

    assertThat(checkInResponse, equalTo(expectedCheckInResponse));
    verifyNoInteractions(circulationStorageService, userService);
  }

  @Test
  void checkInIsForwardedToRequestsMediatedAndLoanDataIsPopulatedInResponse() {
    String itemBarcode = "test_item_barcode";
    String itemId = randomId();
    String instanceId = randomId();

    when(settingsService.isEcsTlrFeatureEnabled())
      .thenReturn(true);

    when(tenantService.isCurrentTenantCentral())
      .thenReturn(true);

    SearchInstance searchInstance = new SearchInstance()
      .id(instanceId)
      .items(List.of(new SearchItem()
        .id(itemId)
        .barcode(itemBarcode)
        .tenantId("data_tenant")));

    when(searchService.findInstanceByItemBarcode(itemBarcode))
      .thenReturn(of(searchInstance));

    CirculationItem circulationItem = new CirculationItem()
      .id(UUID.fromString(itemId))
      .barcode(itemBarcode)
      .status(new CirculationItemStatus().name(CirculationItemStatus.NameEnum.CHECKED_OUT));

    when(circulationItemClient.getCirculationItem(itemId))
      .thenReturn(of(circulationItem));

    when(tenantService.getSecureTenantId())
      .thenReturn(of("secure_tenant"));

    String realUserId = randomId();
    String fakeUserId = randomId();

    Loan loanInSecureTenant = new Loan()
      .id(randomId())
      .itemId(itemId)
      .userId(realUserId)
      .status(new LoanStatus().name("Open"));

    Loan loanInCentralTenant = new Loan()
      .id(randomId())
      .itemId(itemId)
      .userId(fakeUserId)
      .status(new LoanStatus().name("Open"));

    when(circulationStorageService.findOpenLoan(itemId))
      .thenReturn(of(loanInSecureTenant), of(loanInCentralTenant));

    Date checkInDate = new Date();
    UUID servicePointId = UUID.randomUUID();

    CheckInRequest checkInRequest = new CheckInRequest()
      .itemBarcode(itemBarcode)
      .servicePointId(servicePointId)
      .checkInDate(checkInDate);

    CheckInResponse requestMediatedCheckInResponse = new CheckInResponse()
      .loan(new CheckInResponseLoan())
      .item(new CheckInResponseItem()
        .id(itemId)
        .instanceId(instanceId));

    when(requestMediatedClient.checkInByBarcode(checkInRequest))
      .thenReturn(requestMediatedCheckInResponse);

    User fakeUser = new User()
      .id(fakeUserId)
      .barcode("fake_barcode")
      .patronGroup(randomId())
      .personal(new UserPersonal()
        .firstName("fake_first_name")
        .middleName("fake_middle_name")
        .lastName("fake_last_name")
        .preferredFirstName("fake_preferred_first_name"));

    when(userService.find(fakeUserId))
      .thenReturn(fakeUser);

    CheckInResponse checkInResponse = checkInService.checkIn(checkInRequest);

    CheckInResponse expectedCheckInResponse = new CheckInResponse()
      .loan(new CheckInResponseLoan()
        .id(loanInCentralTenant.getId())
        .userId(fakeUserId)
        .borrower(new CheckInResponseLoanBorrower()
          .barcode(fakeUser.getBarcode())
          .patronGroup(fakeUser.getPatronGroup())
          .firstName(fakeUser.getPersonal().getFirstName())
          .middleName(fakeUser.getPersonal().getMiddleName())
          .lastName(fakeUser.getPersonal().getLastName())
          .preferredFirstName(fakeUser.getPersonal().getPreferredFirstName())))
      .item(new CheckInResponseItem()
        .id(itemId)
        .instanceId(instanceId));

    assertThat(checkInResponse, equalTo(expectedCheckInResponse));
  }

}
