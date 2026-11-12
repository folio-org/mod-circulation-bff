package org.folio.circulationbff.domain.model;

import java.util.List;
import java.util.Set;

import org.folio.circulationbff.domain.dto.BffSearchItemCallNumberComponents;
import org.folio.circulationbff.domain.dto.BffSearchItemInTransitDestinationServicePoint;
import org.folio.circulationbff.domain.dto.InstanceContributorsInner;
import org.folio.circulationbff.domain.dto.InstancePublicationInner;

import lombok.Builder;

/**
 * Item and instance fields needed to enrich a loan. Built from Inventory storage records.
 */
@Builder
public record LoanEnrichment(
  // item
  String tenantId,
  String callNumber,
  BffSearchItemCallNumberComponents callNumberComponents,
  String volume,
  String copyNumber,
  String chronology,
  String enumeration,
  String displaySummary,
  String accessionNumber,
  BffSearchItemInTransitDestinationServicePoint inTransitDestinationServicePoint,
  // instance
  Set<String> editions,
  List<String> physicalDescriptions,
  List<InstanceContributorsInner> contributors,
  List<InstancePublicationInner> publication) {
}
