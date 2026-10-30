package org.folio.circulationbff.domain.mapping;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.List;
import java.util.Objects;

import org.folio.circulationbff.domain.dto.InstanceContributorsInner;
import org.folio.circulationbff.domain.dto.InstancePublicationInner;
import org.folio.circulationbff.domain.dto.LoanItem;
import org.folio.circulationbff.domain.model.LoanEnrichment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CirculationLoanMapper {

  // dcb populated fields
  @Mapping(target = "id", source = "loanItem.id")
  @Mapping(target = "instanceId", source = "loanItem.instanceId")
  @Mapping(target = "instanceHrid", source = "loanItem.instanceHrid")
  @Mapping(target = "holdingsRecordId", source = "loanItem.holdingsRecordId")
  @Mapping(target = "title", source = "loanItem.title")
  @Mapping(target = "status", source = "loanItem.status")
  @Mapping(target = "barcode", source = "loanItem.barcode")
  @Mapping(target = "location", source = "loanItem.location")
  @Mapping(target = "materialType", source = "loanItem.materialType")
  // item related fields
  @Mapping(target = "tenantId", source = "enrichment.tenantId")
  @Mapping(target = "callNumber", source = "enrichment.callNumber")
  @Mapping(target = "callNumberComponents", source = "enrichment.callNumberComponents")
  @Mapping(target = "volume", source = "enrichment.volume")
  @Mapping(target = "copyNumber", source = "enrichment.copyNumber")
  @Mapping(target = "chronology", source = "enrichment.chronology")
  @Mapping(target = "enumeration", source = "enrichment.enumeration")
  @Mapping(target = "displaySummary", source = "enrichment.displaySummary")
  @Mapping(target = "accessionNumber", source = "enrichment.accessionNumber")
  @Mapping(target = "inTransitDestinationServicePoint",
    source = "enrichment.inTransitDestinationServicePoint")
  @Mapping(target = "inTransitDestinationServicePointId",
    source = "enrichment.inTransitDestinationServicePoint.id")
  // instance related fields
  @Mapping(target = "editions", source = "enrichment.editions")
  @Mapping(target = "contributors", source = "enrichment.contributors")
  @Mapping(target = "physicalDescriptions", source = "enrichment.physicalDescriptions")
  @Mapping(target = "primaryContributor",
    expression = "java(getPrimaryContributorName(enrichment))")
  @Mapping(target = "datesOfPublication", expression = "java(getDatesOfPublication(enrichment))")
  LoanItem enrichLoanItem(LoanItem loanItem, LoanEnrichment enrichment);

  default String getPrimaryContributorName(LoanEnrichment enrichment) {
    return emptyIfNull(enrichment.contributors()).stream()
      .filter(contributor -> Boolean.TRUE.equals(contributor.getPrimary()))
      .findFirst()
      .map(InstanceContributorsInner::getName)
      .orElse(null);
  }

  default List<String> getDatesOfPublication(LoanEnrichment enrichment) {
    return emptyIfNull(enrichment.publication()).stream()
      .map(InstancePublicationInner::getDateOfPublication)
      .filter(Objects::nonNull)
      .distinct()
      .toList();
  }
}
