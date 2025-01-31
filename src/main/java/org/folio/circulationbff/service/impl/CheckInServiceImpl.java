package org.folio.circulationbff.service.impl;

import static java.util.stream.Collectors.joining;

import java.util.List;

import org.folio.circulationbff.client.feign.CheckInClient;
import org.folio.circulationbff.domain.dto.CheckInRequest;
import org.folio.circulationbff.domain.dto.CheckInResponse;
import org.folio.circulationbff.domain.dto.Contributor;
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
    if (!DCB_INSTANCE_ID.equals(response.getItem().getInstanceId())) {
      log.info("processStaffSlipContext:: keeping staff slip context because it was built " +
        "for inventory item, not for circulation item");
      return;
    }

    log.info("processStaffSlipContext:: rebuilding staff slip context with inventory item " +
      "information");
    rebuildStaffSlipContextWithInventoryItem(response);
  }

  private void rebuildStaffSlipContextWithInventoryItem(CheckInResponse response) {
    var itemId = response.getItem().getId();
    log.info("rebuildStaffSlipContextWithInventoryItem:: item ID: {}", itemId);

    var searchInstance = searchService.findInstanceByItemId(itemId);
    if (searchInstance == null) {
      log.warn("rebuildStaffSlipContextWithInventoryItem:: instance not found");
      return;
    }

    var itemTenantId = getItemTenantId(itemId, searchInstance);
    var item = inventoryService.fetchItem(itemTenantId, itemId);
    if (item == null) {
      log.warn("rebuildStaffSlipContextWithInventoryItem:: item not found, itemId: {}", itemId);
      return;
    }

    var location = inventoryService.fetchLocation(itemTenantId, item.getEffectiveLocationId());
    if (location == null) {
      log.warn("rebuildStaffSlipContextWithInventoryItem:: location not found, locationId: {}",
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
      .allContributors(formatContributorNames(searchInstance.getContributors()))
      .barcode(item.getBarcode())
      .callNumber(item.getItemLevelCallNumber())
      .callNumberPrefix(item.getItemLevelCallNumberPrefix())
      .callNumberSuffix(item.getItemLevelCallNumberSuffix())
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
        fetchInstitutionName(itemTenantId, location.getInstitutionId()))
      .effectiveLocationCampus(fetchCampusName(itemTenantId, location.getCampusId()))
      .effectiveLocationLibrary(fetchLocationLibraryName(itemTenantId, location.getLibraryId()))
      .effectiveLocationSpecific(location.getName());

      log.info("rebuildStaffSlipContextWithInventoryItem:: staff slips context for item {} " +
        "has been successfully rebuilt", itemId);
  }

  private static String formatContributorNames(List<Contributor> contributors) {
    if (contributors == null) {
      log.info("collectAllContributors:: contributors not found");
      return null;
    }
    return contributors.stream()
      .map(Contributor::getName)
      .collect(joining("; "));
  }

  private String fetchInstitutionName(String tenantId, String institutionId) {
    log.info("fetchInstitutionName:: tenantId={}, institutionId={}", tenantId, institutionId);
    var institution = inventoryService.fetchInstitution(tenantId, institutionId);
    if (institution == null) {
      log.warn("fetchInstitutionName:: institution {} not found", institutionId);
      return null;
    }
    String institutionName = institution.getName();
    log.debug("fetchInstitutionName:: result: {}", institutionName);
    return institutionName;
  }

  private String fetchCampusName(String tenantId, String campusId) {
    log.info("fetchCampusName:: tenantId={}, campusId={}", tenantId, campusId);
    var campus = inventoryService.fetchCampus(tenantId, campusId);
    if (campus == null) {
      log.warn("fetchCampusName:: campus {} not found", campusId);
      return null;
    }
    String campusName = campus.getName();
    log.debug("fetchCampusName:: result: {}", campusName);
    return campusName;
  }

  private String fetchLocationLibraryName(String tenantId, String libraryId) {
    log.info("fetchLocationLibraryName:: tenantId={}, libraryId={}", tenantId, libraryId);
    var library = inventoryService.fetchLibrary(tenantId, libraryId);
    if (library == null) {
      log.warn("fetchLocationLibrary:: library {} not found", libraryId);
      return null;
    }
    String libraryName = library.getName();
    log.debug("fetchLocationLibraryName:: result: {}", libraryName);
    return libraryName;
  }

  private String fetchServicePointName(String tenantId, String servicePointId) {
    log.info("fetchServicePointName:: tenantId={}, libraryId={}", tenantId, servicePointId);
    var servicePoint = inventoryService.fetchServicePoint(tenantId, servicePointId);
    if (servicePoint == null) {
      log.warn("fetchServicePointName:: service point {} not found",
        servicePointId);
      return null;
    }
    String servicePointName = servicePoint.getName();
    log.debug("fetchServicePointName:: result: {}", servicePointName);
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
