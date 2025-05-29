package org.folio.circulationbff.util;

import static org.folio.circulationbff.api.BaseIT.TENANT_ID_COLLEGE;
import static org.folio.circulationbff.api.BaseIT.TENANT_ID_CONSORTIUM;
import static org.folio.circulationbff.api.BaseIT.randomId;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.folio.circulationbff.domain.dto.BffSearchInstance;
import org.folio.circulationbff.domain.dto.BffSearchItem;
import org.folio.circulationbff.domain.dto.BffSearchItemCallNumberComponents;
import org.folio.circulationbff.domain.dto.BffSearchItemInTransitDestinationServicePoint;
import org.folio.circulationbff.domain.dto.BffSearchItemMaterialType;
import org.folio.circulationbff.domain.dto.CirculationLoan;
import org.folio.circulationbff.domain.dto.Contributor;
import org.folio.circulationbff.domain.dto.Instance;
import org.folio.circulationbff.domain.dto.InstanceContributorsInner;
import org.folio.circulationbff.domain.dto.InstancePublicationInner;
import org.folio.circulationbff.domain.dto.Item;
import org.folio.circulationbff.domain.dto.ItemEffectiveCallNumberComponents;
import org.folio.circulationbff.domain.dto.LoanItem;
import org.folio.circulationbff.domain.dto.LoanItemContributorsInner;
import org.folio.circulationbff.domain.dto.LoanItemLocation;
import org.folio.circulationbff.domain.dto.LoanItemStatus;
import org.folio.circulationbff.domain.dto.Publication;
import org.folio.circulationbff.domain.dto.SearchInstance;
import org.folio.circulationbff.domain.dto.SearchItem;
import org.folio.circulationbff.domain.dto.SearchItemEffectiveCallNumberComponents;

public class CirculationLoanTestData {

  public static final String LOAN_ID = randomId();
  public static final String USER_ID = randomId();
  public static final String ITEM_ID = randomId();
  public static final String INSTANCE_ID = randomId();
  public static final String HOLDINGS_RECORD_ID = randomId();
  public static final UUID IN_TRANSIT_DESTINATION_SERVICE_POINT_ID = UUID.randomUUID();

  public static CirculationLoan circulationLoan(String userId, boolean isDcb, LoanItem item) {
    return new CirculationLoan()
      .id(LOAN_ID)
      .itemId(ITEM_ID)
      .userId(userId)
      .item(item)
      .isDcb(isDcb);
  }

  public static CirculationLoan circulationLoan(boolean isDcb, LoanItem item) {
    return circulationLoan(USER_ID, isDcb, item);
  }

  public static LoanItem dcbLoanItem() {
    return new LoanItem()
      .id(ITEM_ID)
      .instanceId(INSTANCE_ID)
      .instanceHrid("in00000000001")
      .holdingsRecordId(HOLDINGS_RECORD_ID)
      .title("Test Item")
      .barcode("testbarcode")
      .status(new LoanItemStatus().name("Checked Out").date(Date.from(Instant.ofEpochSecond(1))))
      .location(new LoanItemLocation().name("DCB"))
      .materialType(new BffSearchItemMaterialType().name("text"));
  }

  public static LoanItem enrichedLoanItem() {
    return new LoanItem()
      .id(ITEM_ID)
      .instanceId(INSTANCE_ID)
      .instanceHrid("in00000000001")
      .holdingsRecordId(HOLDINGS_RECORD_ID)
      .title("Test Item")
      .barcode("testbarcode")
      .status(new LoanItemStatus().name("Checked Out").date(Date.from(Instant.ofEpochSecond(1))))
      .location(new LoanItemLocation().name("DCB"))
      .materialType(new BffSearchItemMaterialType().name("text"))
      .tenantId(TENANT_ID_COLLEGE)
      .callNumber("testCallNumber")
      .callNumberComponents(new BffSearchItemCallNumberComponents()
        .prefix("testCallNumber Prefix")
        .callNumber("testCallNumber")
        .suffix("testCallNumber Suffix"))
      .volume("testVolume")
      .copyNumber("testCopyNumber")
      .chronology("testChronology")
      .enumeration("testEnumeration")
      .displaySummary("testDisplaySummary")
      .accessionNumber("testAccessionNumber")
      .datesOfPublication(List.of("1999", "2000"))
      .physicalDescriptions(List.of("testPhysicalDescription1", "testPhysicalDescription2"))
      .inTransitDestinationServicePointId(IN_TRANSIT_DESTINATION_SERVICE_POINT_ID)
      .inTransitDestinationServicePoint(new BffSearchItemInTransitDestinationServicePoint()
        .id(IN_TRANSIT_DESTINATION_SERVICE_POINT_ID).name("test-library"))
      .editions(new LinkedHashSet<>(List.of("edition1", "edition2")))
      .primaryContributor("TestContributor2")
      .contributors(List.of(
        new LoanItemContributorsInner().name("TestContributor1"),
        new LoanItemContributorsInner().name("TestContributor2"),
        new LoanItemContributorsInner().name("TestContributor3")));
  }

  public static SearchItem searchItem() {
    return new SearchItem()
      .id(ITEM_ID)
      .tenantId(TENANT_ID_COLLEGE)
      .effectiveCallNumberComponents(new SearchItemEffectiveCallNumberComponents()
        .prefix("testCallNumber Prefix")
        .callNumber("testCallNumber")
        .suffix("testCallNumber Suffix"))
      .accessionNumber("testAccessionNumber");
  }

  public static Item inventoryItem() {
    return new Item()
      .id(ITEM_ID)
      .effectiveCallNumberComponents(new ItemEffectiveCallNumberComponents()
        .prefix("testCallNumber Prefix")
        .callNumber("testCallNumber")
        .suffix("testCallNumber Suffix"))
      .volume("testVolume")
      .copyNumber("testCopyNumber")
      .chronology("testChronology")
      .enumeration("testEnumeration")
      .displaySummary("testDisplaySummary")
      .accessionNumber("testAccessionNumber")
      .inTransitDestinationServicePointId(IN_TRANSIT_DESTINATION_SERVICE_POINT_ID.toString());
  }

  public static BffSearchItem bffSearchItem() {
    return new BffSearchItem()
      .id(ITEM_ID)
      .tenantId(TENANT_ID_COLLEGE)
      .callNumber("testCallNumber")
      .callNumberComponents(new BffSearchItemCallNumberComponents()
        .prefix("testCallNumber Prefix")
        .callNumber("testCallNumber")
        .suffix("testCallNumber Suffix"))
      .volume("testVolume")
      .copyNumber("testCopyNumber")
      .chronology("testChronology")
      .enumeration("testEnumeration")
      .displaySummary("testDisplaySummary")
      .accessionNumber("testAccessionNumber")
      .inTransitDestinationServicePointId(IN_TRANSIT_DESTINATION_SERVICE_POINT_ID)
      .inTransitDestinationServicePoint(new BffSearchItemInTransitDestinationServicePoint()
        .id(IN_TRANSIT_DESTINATION_SERVICE_POINT_ID).name("test-library"));
  }

  public static BffSearchInstance bffSearchInstance(BffSearchItem... items) {
    return new BffSearchInstance()
      .id(INSTANCE_ID)
      .tenantId(TENANT_ID_CONSORTIUM)
      .physicalDescriptions(List.of("testPhysicalDescription1", "testPhysicalDescription2"))
      .editions(new LinkedHashSet<>(List.of("edition1", "edition2")))
      .publication(List.of(
        new Publication().publisher("testPublisher1").dateOfPublication("1999"),
        new Publication().publisher("testPublisher2").dateOfPublication("2000")))
      .contributors(List.of(
        new Contributor().name("TestContributor1").primary(false),
        new Contributor().name("TestContributor2").primary(true),
        new Contributor().name("TestContributor3").primary(false)))
      .items(List.of(items));
  }

  public static SearchInstance searchInstance(SearchItem... items) {
    return new SearchInstance()
      .id(INSTANCE_ID)
      .tenantId(TENANT_ID_CONSORTIUM)
      .publication(List.of(
        new Publication().publisher("testPublisher1").dateOfPublication("1999"),
        new Publication().publisher("testPublisher2").dateOfPublication("2000")))
      .contributors(List.of(
        new Contributor().name("TestContributor1").primary(false),
        new Contributor().name("TestContributor2").primary(true),
        new Contributor().name("TestContributor3").primary(false)))
      .items(List.of(items));
  }

  public static Instance inventoryInstance() {
    return new Instance()
      .id(INSTANCE_ID)
      .editions(new LinkedHashSet<>(List.of("edition1", "edition2")))
      .physicalDescriptions(List.of("physicalDescription1", "physicalDescription2"))
      .publication(List.of(
        new InstancePublicationInner().publisher("testPublisher1").dateOfPublication("1999"),
        new InstancePublicationInner().publisher("testPublisher2").dateOfPublication("2000")))
      .physicalDescriptions(List.of("testPhysicalDescription1", "testPhysicalDescription2"))
      .contributors(List.of(
        new InstanceContributorsInner().name("TestContributor1").primary(false),
        new InstanceContributorsInner().name("TestContributor2").primary(true),
        new InstanceContributorsInner().name("TestContributor3").primary(false)));
  }
}
