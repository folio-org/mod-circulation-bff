package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.Location;
import org.folio.circulationbff.domain.dto.ServicePoint;

public interface InventoryService {
  Item fetchItem(String id);
  Location fetchLocation(String id);
  ServicePoint fetchServicePoint(String id);
}
