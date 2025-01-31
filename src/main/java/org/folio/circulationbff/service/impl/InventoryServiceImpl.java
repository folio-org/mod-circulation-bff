package org.folio.circulationbff.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.circulationbff.client.feign.ItemStorageClient;
import org.folio.circulationbff.client.feign.LocationCampusClient;
import org.folio.circulationbff.client.feign.LocationClient;
import org.folio.circulationbff.client.feign.LocationInstitutionClient;
import org.folio.circulationbff.client.feign.LocationLibraryClient;
import org.folio.circulationbff.client.feign.ServicePointClient;
import org.folio.circulationbff.domain.dto.Campus;
import org.folio.circulationbff.domain.dto.Institution;
import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.Library;
import org.folio.circulationbff.domain.dto.Location;
import org.folio.circulationbff.domain.dto.ServicePoint;
import org.folio.circulationbff.service.InventoryService;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class InventoryServiceImpl implements InventoryService {
  private final ItemStorageClient itemClient;
  private final LocationClient locationClient;
  private final ServicePointClient servicePointClient;
  private final LocationLibraryClient libraryClient;
  private final LocationInstitutionClient institutionClient;
  private final LocationCampusClient campusClient;
  private final SystemUserScopedExecutionService executionService;

  @Override
  public Item fetchItem(String tenantId, String id) {
    log.info("fetchItem:: fetching item {} from tenant {}", id, tenantId);
    return executionService.executeSystemUserScoped(tenantId, () -> itemClient.findItem(id));
  }

  @Override
  public Location fetchLocation(String tenantId, String id) {
    log.info("fetchLocation:: fetching location {} from tenant {}", id, tenantId);
    return executionService.executeSystemUserScoped(tenantId, () -> locationClient.findLocation(id));
  }

  @Override
  public ServicePoint fetchServicePoint(String tenantId, String id) {
    log.info("fetchServicePoint:: fetching service point {} from tenant {}", id, tenantId);
    return executionService.executeSystemUserScoped(tenantId,
      () -> servicePointClient.findServicePoint(id));
  }

  @Override
  public Campus fetchCampus(String tenantId, String id) {
    log.info("fetchCampus:: fetching campus {} from tenant {}", id, tenantId);
    return executionService.executeSystemUserScoped(tenantId, () -> campusClient.findCampus(id));
  }

  @Override
  public Institution fetchInstitution(String tenantId, String id) {
    log.info("fetchInstitution:: fetching institution {} from tenant {}", id, tenantId);
    return executionService.executeSystemUserScoped(tenantId,
      () -> institutionClient.findInstitution(id));
  }

  @Override
  public Library fetchLibrary(String tenantId, String id) {
    log.info("fetchLibrary:: fetching library {} from tenant {}", id, tenantId);
    return executionService.executeSystemUserScoped(tenantId, () -> libraryClient.findLibrary(id));
  }
}
