package org.folio.circulationbff.service.impl;

import static java.util.stream.Collectors.joining;
import static org.folio.circulationbff.domain.dto.CirculationItemStatus.NameEnum.CHECKED_OUT;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.folio.circulationbff.client.feign.CheckInClient;
import org.folio.circulationbff.client.feign.CirculationItemClient;
import org.folio.circulationbff.client.feign.HoldingsStorageClient;
import org.folio.circulationbff.domain.dto.CheckInRequest;
import org.folio.circulationbff.domain.dto.CheckInResponse;
import org.folio.circulationbff.domain.dto.CheckInResponseItem;
import org.folio.circulationbff.domain.dto.CheckInResponseItemInTransitDestinationServicePoint;
import org.folio.circulationbff.domain.dto.CheckInResponseLoan;
import org.folio.circulationbff.domain.dto.CirculationItem;
import org.folio.circulationbff.domain.dto.CirculationItemStatus;
import org.folio.circulationbff.domain.dto.Contributor;
import org.folio.circulationbff.domain.dto.HoldingsRecord;
import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.Location;
import org.folio.circulationbff.domain.dto.SearchInstance;
import org.folio.circulationbff.domain.dto.SearchItem;
import org.folio.circulationbff.domain.dto.ServicePoint;
import org.folio.circulationbff.service.CheckInService;
import org.folio.circulationbff.service.CirculationStorageService;
import org.folio.circulationbff.service.InventoryService;
import org.folio.circulationbff.service.SearchService;
import org.folio.circulationbff.service.SettingsService;
import org.folio.circulationbff.service.TenantService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class CheckInServiceImpl implements CheckInService {

  private final CheckInClient checkInClient;
  private final CirculationItemClient circulationItemClient;
  private final HoldingsStorageClient holdingsStorageClient;
  private final SettingsService settingsService;
  private final TenantService tenantService;
  private final SearchService searchService;
  private final InventoryService inventoryService;
  private final CirculationStorageService circulationStorageService;
  private final SystemUserScopedExecutionService executionService;
  private static final String DCB_INSTANCE_ID = "9d1b77e4-f02e-4b7f-b296-3f2042ddac54";
  private static final String DCB_SERVICE_POINT_ID = "9d1b77e8-f02e-4b7f-b296-3f2042ddac54";

  @Override
  public CheckInResponse checkIn(CheckInRequest request) {
    CheckInResponse response = null;
    if (settingsService.isEcsTlrFeatureEnabled() && tenantService.isCurrentTenantCentral()) {
      SearchItem item = findItem(request);
      if (item != null) {
        CirculationItem circItem = findCirculationItem(item.getId());
        String secureTenantId = tenantService.getSecureTenantId().orElse(null);
        if (circItem == null) {
          response = checkInRemotely(request, item.getTenantId());
        } else if (shouldCloseLoanInSecureTenant(circItem, secureTenantId)) {
          response = checkInRemotely(request, secureTenantId);
          log.info("checkIn:: secure check-in response: {}", response);
          CheckInResponseLoan loan = response.getLoan();
          loan.id(null)
            .userId(null)
            .getAdditionalProperties().remove("borrower");
          log.info("checkIn:: obfuscated check-in response: {}", response);
        }
      }
    }

    if (response == null) {
      response = doCheckIn(request);
    }

    processCheckInResponse(response);
    return response;
  }

  private boolean shouldCloseLoanInSecureTenant(CirculationItem circItem, String secureTenantId) {
    return secureTenantId != null && circulationItemIsInStatus(circItem, CHECKED_OUT) &&
        openLoanExists(circItem.getId().toString(), secureTenantId);
  }

  private boolean openLoanExists(String itemId, String tenantId) {
    return executionService.executeSystemUserScoped(tenantId,
        () -> circulationStorageService.findOpenLoan(itemId))
      .isPresent();
  }

  private SearchItem findItem(CheckInRequest request) {
    if (request == null) {
      log.info("findItem:: check in request is null");
      return null;
    }

    var itemBarcode = request.getItemBarcode();
    if (itemBarcode == null) {
      log.info("findItem:: itemBarcode is null");
      return null;
    }

    log.info("findItem:: searching for instance by item barcode {}", itemBarcode);

    // TODO: Duplication - search endpoint called twice
    var searchInstanceOptional = searchService.findInstanceByItemBarcode(itemBarcode);
    if (searchInstanceOptional.isEmpty()) {
      log.info("findItem:: failed to find an instance by item barcode {}",
        itemBarcode);
      return null;
    }
    var searchInstance = searchInstanceOptional.get();

    log.info("findItem:: found instance {}", searchInstance::getId);

    var searchItem = getItemFromSearchInstanceByBarcode(searchInstance, itemBarcode);
    if (searchItem == null) {
      log.info("findItem:: failed to find an item by item barcode {}",
        itemBarcode);
      return null;
    }

    log.info("findItem:: found item {} by barcode {} in the instance {}",
      searchItem::getId, () -> itemBarcode, searchInstance::getId);
    return searchItem;
  }

  private CirculationItem findCirculationItem(String itemId) {
    log.info("findCirculationItem:: fetching circulation item {}", itemId);
    Optional<CirculationItem> circulationItem = circulationItemClient.getCirculationItem(itemId);
    log.info("findCirculationItem:: circulation item {} found: {}", itemId, circulationItem.isPresent());

    return circulationItem.orElse(null);
  }

  private CheckInResponse checkInRemotely(CheckInRequest request, String tenantId) {
    log.info("checkInRemotely:: checking in item {} in tenant '{}'", request.getItemBarcode(), tenantId);
    return executionService.executeSystemUserScoped(tenantId, () -> doCheckIn(request));
  }

  private CheckInResponse doCheckIn(CheckInRequest request) {
    log.info("doCheckIn:: checking in item with barcode {}", request::getItemBarcode);
    return checkInClient.checkIn(request);
  }

  private SearchItem getItemFromSearchInstanceByBarcode(SearchInstance searchInstance,
    String itemBarcode) {

    if (searchInstance == null) {
      log.info("getItemFromSearchInstanceByBarcode:: failed to find item {}, instance is null",
        itemBarcode);
      return null;
    }

    log.info("getItemFromSearchInstanceByBarcode:: getting item info by barcode {} " +
        "from instance {}", () -> itemBarcode, searchInstance::getId);

    // TODO: Check in with wildcard item barcode lookup is currently not supported by this module
    var itemBarcodeStripped = StringUtils.strip(itemBarcode, "*");

    return searchInstance.getItems().stream()
      .filter(item -> itemBarcodeStripped.equals(item.getBarcode()))
      .findFirst()
      .orElse(null);
  }

  private void processCheckInResponse(CheckInResponse response) {
    if (!DCB_INSTANCE_ID.equals(response.getItem().getInstanceId())) {
      log.info("processCheckInResponse:: keeping check in response because it was built " +
        "for inventory item, not for circulation item");
      return;
    }

    log.info("processCheckInResponse:: rebuilding check in response with inventory item " +
      "information");
    rebuildCheckInResponseWithInventoryItem(response);
  }

  private void rebuildCheckInResponseWithInventoryItem(CheckInResponse response) {
    var itemId = response.getItem().getId();
    log.info("rebuildCheckInResponseWithInventoryItem:: item ID: {}", itemId);

    var searchInstance = searchService.findInstanceByItemId(itemId);
    if (searchInstance == null) {
      log.warn("rebuildCheckInResponseWithInventoryItem:: instance not found");
      return;
    }

    String itemTenantId = getItemTenantId(itemId, searchInstance);
    if (itemTenantId != null) {
      executionService.executeAsyncSystemUserScoped(itemTenantId,
        () -> rebuildCheckInResponseWithInventoryItem(response, itemId, searchInstance));
    }
  }

  private void rebuildCheckInResponseWithInventoryItem(CheckInResponse response, String itemId,
    SearchInstance searchInstance) {

    log.info("rebuildCheckInResponseWithInventoryItem:: rebuilding staff slip context " +
      "for item {}", itemId);
    var item = inventoryService.fetchItem(itemId);
    if (item == null) {
      log.warn("rebuildCheckInResponseWithInventoryItem:: item {} not found", itemId);
      return;
    }

    var location = inventoryService.fetchLocation(item.getEffectiveLocationId());
    if (location == null) {
      log.warn("rebuildCheckInResponseWithInventoryItem:: location {} not found",
        item.getEffectiveLocationId());
      return;
    }
    var primaryServicePoint = fetchServicePoint(location.getPrimaryServicePoint().toString());

    rebuildCheckInStaffSlipContext(response, searchInstance, item, primaryServicePoint, location);
    rebuildCheckInItem(response, searchInstance, item, primaryServicePoint, location);
    rebuildCheckInLoan(response, searchInstance, item, primaryServicePoint, location);
  }

  private void rebuildCheckInStaffSlipContext(CheckInResponse response, SearchInstance searchInstance,
    Item item, ServicePoint servicePoint, Location location) {

    log.info("rebuildCheckInStaffSlipContext:: rebuilding context with inventory item {}",
      item::getId);

    String servicePointName = servicePoint != null
      ? servicePoint.getName()
      : null;

    response.getStaffSlipContext()
      .getItem()
      .effectiveLocationPrimaryServicePointName(servicePointName)
      .toServicePoint(servicePointName)
      .fromServicePoint(item.getLastCheckIn() != null
        ? fetchServicePointName(item.getLastCheckIn().getServicePointId())
        : null)
      .title(searchInstance.getTitle())
      .primaryContributor(getPrimaryContributorName(searchInstance.getContributors()))
      .allContributors(formatContributorNames(searchInstance.getContributors()))
      .barcode(item.getBarcode())
      .copy(item.getCopyNumber())
      .displaySummary(item.getDisplaySummary())
      .enumeration(item.getEnumeration())
      .volume(item.getVolume())
      .chronology(item.getChronology())
      .yearCaption(item.getYearCaption() != null
        ? String.join("; ", item.getYearCaption())
        : null)
      .loanType(item.getPermanentLoanTypeId())
      .materialType(item.getMaterialTypeId())
      .numberOfPieces(item.getNumberOfPieces())
      .descriptionOfPieces(item.getDescriptionOfPieces())
      .lastCheckedInDateTime(item.getLastCheckIn() != null
        ? item.getLastCheckIn().getDateTime()
        : null)
      .effectiveLocationInstitution(
        fetchInstitutionName(location.getInstitutionId()))
      .effectiveLocationCampus(fetchCampusName(location.getCampusId()))
      .effectiveLocationLibrary(fetchLocationLibraryName(location.getLibraryId()))
      .effectiveLocationSpecific(location.getName());

    if (item.getItemLevelCallNumber() != null) {
      response.getStaffSlipContext().getItem()
        .callNumber(item.getItemLevelCallNumber())
        .callNumberPrefix(item.getItemLevelCallNumberPrefix())
        .callNumberSuffix(item.getItemLevelCallNumberSuffix());
    } else {
      HoldingsRecord holdingsRecord = holdingsStorageClient.findHoldingsRecord(item.getHoldingsRecordId());
      response.getStaffSlipContext().getItem()
        .callNumber(holdingsRecord.getCallNumber())
        .callNumberPrefix(holdingsRecord.getCallNumberPrefix())
        .callNumberSuffix(holdingsRecord.getCallNumberSuffix());
    }

    log.info("rebuildCheckInStaffSlipContext:: staff slips context for item {} " +
      "has been successfully built", item::getId);
  }

  private void rebuildCheckInItem(CheckInResponse response, SearchInstance searchInstance,
    Item item, ServicePoint primaryServicePoint, Location location) {

    CheckInResponseItem checkInItem = response.getItem();
    if (checkInItem == null) {
      log.warn("rebuildCheckInItem:: item in checkInResponse not found");
      return;
    }

    handleInTransitDestinationServicePoint(checkInItem::getInTransitDestinationServicePointId,
      checkInItem::setInTransitDestinationServicePointId,
      checkInItem::getInTransitDestinationServicePoint,
      checkInItem::inTransitDestinationServicePoint, primaryServicePoint, location);

    replaceIfExists(checkInItem::getLocation,
      checkInItem.getLocation() == null ? null : checkInItem.getLocation()::setName,
      location.getName());
    replaceIfExists(checkInItem::getHoldingsRecordId, checkInItem::setHoldingsRecordId,
      item.getHoldingsRecordId());
    replaceIfExists(checkInItem::getId, checkInItem::setInstanceId, searchInstance.getId());

    log.info("rebuildCheckInItem:: checkInItem {} has been successfully built", item::getId);
  }

  private void rebuildCheckInLoan(CheckInResponse response, SearchInstance searchInstance,
    Item item, ServicePoint primaryServicePoint, Location location) {

    var checkInLoan = response.getLoan();
    if (checkInLoan == null) {
      log.info("rebuildCheckInLoan:: loan in checkInResponse not found");
      return;
    }

    var loanItem = checkInLoan.getItem();
    if (loanItem != null) {
      log.info("rebuildCheckInLoan:: checkInLoanItem is present");

      handleInTransitDestinationServicePoint(loanItem::getInTransitDestinationServicePointId,
        loanItem::setInTransitDestinationServicePointId,
        loanItem::getInTransitDestinationServicePoint,
        loanItem::inTransitDestinationServicePoint, primaryServicePoint, location);

      replaceIfExists(loanItem::getLocation,
        loanItem.getLocation() == null ? null : loanItem.getLocation()::setName,
        location.getName());
      replaceIfExists(loanItem::getHoldingsRecordId, loanItem::setHoldingsRecordId,
        item.getHoldingsRecordId());
      replaceIfExists(loanItem::getId, loanItem::setInstanceId, searchInstance.getId());
    }

    log.info("rebuildCheckInLoan:: checkInLoan {} has been successfully built", checkInLoan::getId);
  }

  private void handleInTransitDestinationServicePoint(Supplier<String> getDestinationServicePointId,
    Consumer<String> setDestinationServicePointId,
    Supplier<CheckInResponseItemInTransitDestinationServicePoint> getDestinationServicePoint,
    Consumer<CheckInResponseItemInTransitDestinationServicePoint> setDestinationServicePoint,
    ServicePoint primaryServicePoint, Location location) {

    if (DCB_SERVICE_POINT_ID.equals(getDestinationServicePointId.get())) {
      replaceIfExists(getDestinationServicePointId, setDestinationServicePointId,
        location.getPrimaryServicePoint().toString());

      if (primaryServicePoint != null) {
        replaceIfExists(getDestinationServicePoint, setDestinationServicePoint,
          new CheckInResponseItemInTransitDestinationServicePoint()
            .id(primaryServicePoint.getId())
            .name(primaryServicePoint.getName()));
      }
    }
  }

  private static String formatContributorNames(List<Contributor> contributors) {
    if (contributors == null) {
      log.info("formatContributorNames:: contributors not found");
      return null;
    }
    return contributors.stream()
      .map(Contributor::getName)
      .collect(joining("; "));
  }

  private String fetchInstitutionName(String institutionId) {
    log.info("fetchInstitutionName:: institutionId={}", institutionId);
    var institution = inventoryService.fetchInstitution(institutionId);
    if (institution == null) {
      log.warn("fetchInstitutionName:: institution {} not found", institutionId);
      return null;
    }
    String institutionName = institution.getName();
    log.debug("fetchInstitutionName:: result: {}", institutionName);
    return institutionName;
  }

  private String fetchCampusName(String campusId) {
    log.info("fetchCampusName:: campusId={}", campusId);
    var campus = inventoryService.fetchCampus(campusId);
    if (campus == null) {
      log.warn("fetchCampusName:: campus {} not found", campusId);
      return null;
    }
    String campusName = campus.getName();
    log.debug("fetchCampusName:: result: {}", campusName);
    return campusName;
  }

  private String fetchLocationLibraryName(String libraryId) {
    log.info("fetchLocationLibraryName:: libraryId={}", libraryId);
    var library = inventoryService.fetchLibrary(libraryId);
    if (library == null) {
      log.warn("fetchLocationLibrary:: library {} not found", libraryId);
      return null;
    }
    String libraryName = library.getName();
    log.debug("fetchLocationLibraryName:: result: {}", libraryName);
    return libraryName;
  }

  private String fetchServicePointName(String servicePointId) {
    log.info("fetchServicePointName:: libraryId={}", servicePointId);
    var servicePoint = fetchServicePoint(servicePointId);
    if (servicePoint == null) {
      log.warn("fetchServicePointName:: service point {} not found", servicePointId);
      return null;
    }
    String servicePointName = servicePoint.getName();
    log.info("fetchServicePointName:: result: {}", servicePointName);

    return servicePointName;
  }

  private ServicePoint fetchServicePoint(String servicePointId) {
    log.info("fetchServicePoint:: libraryId={}", servicePointId);
    var servicePoint = inventoryService.fetchServicePoint(servicePointId);
    log.info("fetchServicePoint:: result: {}", servicePoint);

    return servicePoint;
  }

  private String getItemTenantId(String itemId, SearchInstance searchInstance) {
    var tenantId = searchInstance.getItems()
      .stream()
      .filter(item -> item.getId().equals(itemId))
      .findFirst()
      .map(SearchItem::getTenantId)
      .orElse(null);
    log.info("getItemTenantId:: tenantId: {}", tenantId);

    return tenantId;
  }

  public String getPrimaryContributorName(List<Contributor> contributors) {
    if (contributors == null) {
      log.info("getPrimaryContributorName:: contributors not found");
      return null;
    }

    return contributors.stream()
      .filter(Contributor::getPrimary)
      .findFirst()
      .map(Contributor::getName)
      .orElse(null);
  }

  private <T, S> void replaceIfExists(Supplier<T> nonNullCheckGetter, Consumer<S> setter, S value) {
    if (nonNullCheckGetter == null || setter == null) {
      log.info("replaceIfExists:: null getter or setter");
      return;
    }

    if (nonNullCheckGetter.get() != null) {
      setter.accept(value);
    }
  }

  private static boolean circulationItemIsInStatus(CirculationItem circulationItem,
    CirculationItemStatus.NameEnum expectedStatus) {

    log.info("circulationItemIsInStatus:: checking if circulation item {} is in status {}",
      circulationItem::getId, expectedStatus::getValue);

    boolean result = Optional.of(circulationItem)
      .map(CirculationItem::getStatus)
      .map(CirculationItemStatus::getName)
      .map(actualStatus -> expectedStatus == actualStatus)
      .orElse(false);

    log.info("circulationItemIsInStatus:: result: {}", result);
    return result;
  }
}
