package org.folio.circulationbff.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.folio.circulationbff.client.feign.CheckInClient;
import org.folio.circulationbff.domain.dto.CheckInRequest;
import org.folio.circulationbff.domain.dto.CheckInResponse;
import org.folio.circulationbff.domain.dto.Contributor;
import org.folio.circulationbff.domain.dto.Location;
import org.folio.circulationbff.domain.dto.SearchInstance;
import org.folio.circulationbff.domain.dto.SearchItem;
import org.folio.circulationbff.service.CheckInService;
import org.folio.circulationbff.service.InventoryService;
import org.folio.circulationbff.service.SearchService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class CheckInServiceImpl implements CheckInService {

  private final CheckInClient checkInClient;
  private final SearchService searchService;
  private final InventoryService inventoryService;
  private static final String DCB_INSTANCE_ID = "9d1b77e4-f02e-4b7f-b296-3f2042ddac54";

  @Override
  public CheckInResponse checkIn(CheckInRequest request) {
    log.info("checkIn: checking in item with barcode {} on service point {}",
      request.getItemBarcode(), request.getServicePointId());
    var response = checkInClient.checkIn(request);
    processStaffSlipContext(response);

    return response;
  }

  private void processStaffSlipContext(CheckInResponse response) {
    if (!isStaffSlipContextForDcbItem(response)) {
      log.info("processStaffSlipContext:: staff slip context is not for DCB item");
      return;
    }

    fillWithRealStaffSlipContext(response);
  }

  private void fillWithRealStaffSlipContext(CheckInResponse response) {
    var itemId = response.getItem().getId();
    log.info("fillWithRealStaffSlipContext:: filling staff slip context for item {}", itemId);

    var searchInstance = searchService.findInstanceByItemId(itemId);
    if (searchInstance == null) {
      log.warn("fillWithRealStaffSlipContext:: instance not found");
      return;
    }

    var itemTenantId = getItemTenantId(itemId, searchInstance);
    var item = inventoryService.fetchItem(itemTenantId, itemId);
    if (item == null) {
      log.warn("fillWithRealStaffSlipContext:: item not found, itemId: {}", itemId);
      return;
    }

    var location = inventoryService.fetchLocation(itemTenantId, item.getEffectiveLocationId());
    if (location == null) {
      log.warn("fillWithRealStaffSlipContext:: location not found, locationId: {}",
        item.getEffectiveLocationId());
      return;
    }
    var servicePointName = fetchServicePointName(itemTenantId, location.getPrimaryServicePoint()
      .toString());

    response.getStaffSlipContext()
      .getItem()
      .effectiveLocationPrimaryServicePointName(servicePointName)
      .toServicePoint(servicePointName)
      .fromServicePoint(item.getLastCheckIn() != null
        ? fetchServicePointName(itemTenantId, item.getLastCheckIn().getServicePointId())
        : null)
      .title(searchInstance.getTitle())
      .primaryContributor(getPrimaryContributorName(searchInstance.getContributors()))
      .allContributors(collectAllContributors(searchInstance))
      .barcode(item.getBarcode())
      .callNumber(item.getItemLevelCallNumber())
      .callNumberPrefix(item.getItemLevelCallNumberPrefix())
      .callNumberSuffix(item.getItemLevelCallNumberSuffix())
      .copy(item.getCopyNumber())
      .displaySummary(item.getDisplaySummary())
      .enumeration(item.getEnumeration())
      .volume(item.getVolume())
      .chronology(item.getChronology())
      .yearCaption(item.getYearCaption() != null ? String.join("; ", item.getYearCaption()) : null)
      .loanType(item.getPermanentLoanTypeId())
      .materialType(item.getMaterialTypeId())
      .numberOfPieces(item.getNumberOfPieces())
      .descriptionOfPieces(item.getDescriptionOfPieces())
      .lastCheckedInDateTime(item.getLastCheckIn() != null ? item.getLastCheckIn().getDateTime() : null)
      .effectiveLocationInstitution(fetchInstitutionName(itemTenantId, location))
      .effectiveLocationCampus(fetchCampusName(itemTenantId, location))
      .effectiveLocationLibrary(fetchLocationLibraryName(itemTenantId, location))
      .effectiveLocationSpecific(location.getName());
  }

  private static String collectAllContributors(SearchInstance searchInstance) {
    if (searchInstance.getContributors() == null) {
      log.info("collectAllContributors:: contributors not found in searchInstance {}",
        searchInstance.getId());
      return null;
    }
    return searchInstance.getContributors().stream()
      .map(Contributor::getName)
      .map(contributorName -> contributorName + "; ")
      .collect(Collectors.joining(""));
  }

  private boolean isStaffSlipContextForDcbItem(CheckInResponse response) {
    return DCB_INSTANCE_ID.equals(response.getItem().getInstanceId());
  }

  private String fetchInstitutionName(String itemTenantId, Location location) {
    var institution = inventoryService.fetchInstitution(itemTenantId, location.getInstitutionId());
    if (institution == null) {
      log.warn("fetchInstitutionName:: institution is not found {}", location.getInstitutionId());
      return null;
    }

    return institution.getName();
  }

  private String fetchCampusName(String itemTenantId, Location location) {
    var campus = inventoryService.fetchCampus(itemTenantId, location.getCampusId());
    if (campus == null) {
      log.warn("fetchCampusName:: campus is not found {}", location.getCampusId());
      return null;
    }

    return campus.getName();
  }

  private String fetchLocationLibraryName(String itemTenantId, Location location) {
    var library = inventoryService.fetchLibrary(itemTenantId, location.getLibraryId());
    if (library == null) {
      log.warn("fetchLocationLibrary:: library is not found {}", location.getLibraryId());
      return null;
    }

    return library.getName();
  }

  private String fetchServicePointName(String tenantId, String servicePointId) {
    var servicePoint = inventoryService.fetchServicePoint(tenantId, servicePointId);
    if (servicePoint == null) {
      log.warn("fetchServicePointName:: servicePoint not found, servicePointId: {}",
        servicePointId);
      return null;
    }
    String servicePointName = servicePoint.getName();
    log.info("fetchServicePointName:: result: {}", servicePointName);

    return servicePointName;
  }

  private String getItemTenantId(String itemId, SearchInstance searchInstance) {
    return searchInstance.getItems()
      .stream()
      .filter(item -> item.getId().equals(itemId))
      .findFirst()
      .map(SearchItem::getTenantId)
      .orElse(null);
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

}
