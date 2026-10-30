package org.folio.circulationbff.domain.mapping;

import static org.folio.circulationbff.util.CirculationLoanTestData.dcbLoanItem;
import static org.folio.circulationbff.util.CirculationLoanTestData.enrichedLoanItem;
import static org.folio.circulationbff.util.CirculationLoanTestData.loanEnrichment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.folio.circulationbff.domain.dto.InstanceContributorsInner;
import org.folio.circulationbff.domain.model.LoanEnrichment;
import org.junit.jupiter.api.Test;

class CirculationLoanMapperTest {

  private final CirculationLoanMapper mapper = new CirculationLoanMapperImpl();

  @Test
  void enrichLoanItemCopiesItemAndInstanceFields() {
    var result = mapper.enrichLoanItem(dcbLoanItem(), loanEnrichment());

    assertEquals(enrichedLoanItem(), result);
  }

  @Test
  void enrichLoanItemKeepsDcbFieldsFromLoanItem() {
    var result = mapper.enrichLoanItem(dcbLoanItem(), loanEnrichment());

    assertEquals(dcbLoanItem().getTitle(), result.getTitle());
    assertEquals(dcbLoanItem().getBarcode(), result.getBarcode());
    assertEquals(dcbLoanItem().getLocation(), result.getLocation());
  }

  @Test
  void enrichLoanItemWithoutPrimaryContributorAndPublication() {
    var enrichment = LoanEnrichment.builder()
      .tenantId("college")
      .editions(Set.of())
      .physicalDescriptions(List.of())
      .contributors(List.of(new InstanceContributorsInner().name("A").primary(false)))
      .publication(List.of())
      .build();

    var result = mapper.enrichLoanItem(dcbLoanItem(), enrichment);

    assertNull(result.getPrimaryContributor());
    assertTrue(result.getDatesOfPublication().isEmpty());
    assertEquals("A", result.getContributors().get(0).getName());
  }

  @Test
  void enrichLoanItemToleratesNullPrimaryFlag() {
    var enrichment = LoanEnrichment.builder()
      .tenantId("college")
      .contributors(List.of(new InstanceContributorsInner().name("A")))
      .build();

    var result = mapper.enrichLoanItem(dcbLoanItem(), enrichment);

    assertNull(result.getPrimaryContributor());
  }
}
