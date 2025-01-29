package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.Campus;
import org.folio.circulationbff.domain.dto.Institution;
import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.Library;
import org.folio.circulationbff.domain.dto.Location;
import org.folio.circulationbff.domain.dto.ServicePoint;

public interface InventoryService {
  Item fetchItem(String tenantId, String id);
  Location fetchLocation(String tenantId, String id);
  ServicePoint fetchServicePoint(String tenantId, String id);
  Campus fetchCampus(String tenantId, String id);
  Institution fetchInstitution(String tenantId, String id);
  Library fetchLibrary(String tenantId, String id);
}
