package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.Campus;
import org.folio.circulationbff.domain.dto.Institution;
import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.Library;
import org.folio.circulationbff.domain.dto.Location;
import org.folio.circulationbff.domain.dto.ServicePoint;

public interface InventoryService {
  Item fetchItem(String id);
  Location fetchLocation(String id);
  ServicePoint fetchServicePoint(String id);
  Campus fetchCampus(String id);
  Institution fetchInstitution(String id);
  Library fetchLibrary(String id);
}
