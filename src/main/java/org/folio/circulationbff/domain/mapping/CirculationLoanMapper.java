package org.folio.circulationbff.domain.mapping;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.folio.circulationbff.domain.dto.BffSearchInstance;
import org.folio.circulationbff.domain.dto.BffSearchItem;
import org.folio.circulationbff.domain.dto.Contributor;
import org.folio.circulationbff.domain.dto.LoanItem;
import org.folio.circulationbff.domain.dto.Publication;
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
  @Mapping(target = "tenantId", source = "item.tenantId")
  @Mapping(target = "callNumber", source = "item.callNumber")
  @Mapping(target = "callNumberComponents", source = "item.callNumberComponents")
  @Mapping(target = "volume", source = "item.volume")
  @Mapping(target = "copyNumber", source = "item.copyNumber")
  @Mapping(target = "chronology", source = "item.chronology")
  @Mapping(target = "enumeration", source = "item.enumeration")
  @Mapping(target = "displaySummary", source = "item.displaySummary")
  @Mapping(target = "accessionNumber", source = "item.accessionNumber")
  @Mapping(target = "inTransitDestinationServicePoint", source = "item.inTransitDestinationServicePoint")
  @Mapping(target = "inTransitDestinationServicePointId", source = "item.inTransitDestinationServicePoint.id")
  // instance related fields
  @Mapping(target = "editions", source = "instance.editions")
  @Mapping(target = "contributors", source = "instance.contributors")
  @Mapping(target = "physicalDescriptions", source = "instance.physicalDescriptions")
  @Mapping(target = "primaryContributor", expression = "java(getPrimaryContributorName(instance))")
  @Mapping(target = "datesOfPublication", expression = "java(getDatesOfPublication(instance))")
  LoanItem enrichLoan(LoanItem loanItem, BffSearchInstance instance, BffSearchItem item);

  default String getPrimaryContributorName(BffSearchInstance searchInstance) {
    return toStream(searchInstance.getContributors())
      .filter(Contributor::getPrimary)
      .findFirst()
      .map(Contributor::getName)
      .orElse(null);
  }

  default List<String> getDatesOfPublication(BffSearchInstance searchInstance) {
    return Optional.ofNullable(searchInstance.getPublication())
      .stream()
      .flatMap(Collection::stream)
      .map(Publication::getDateOfPublication)
      .distinct()
      .toList();
  }

  static <T> Stream<T> toStream(Collection<T> collection) {
    return CollectionUtils.isEmpty(collection) ? Stream.empty() : collection.stream();
  }
}
