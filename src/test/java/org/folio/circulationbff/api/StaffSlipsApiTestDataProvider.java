package org.folio.circulationbff.api;

import static org.folio.circulationbff.api.BaseIT.TENANT_ID_CONSORTIUM;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.folio.circulationbff.domain.dto.CirculationSettings;
import org.folio.circulationbff.domain.dto.CirculationSettingsResponse;
import org.folio.circulationbff.domain.dto.CirculationSettingsValue;
import org.folio.circulationbff.domain.dto.StaffSlip;
import org.folio.circulationbff.domain.dto.StaffSlipCollection;
import org.folio.circulationbff.domain.dto.TlrSettings;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.folio.circulationbff.domain.dto.UserTenantCollection;
import org.junit.jupiter.params.provider.Arguments;

public class StaffSlipsApiTestDataProvider {

  private static final String CIRCULATION_BFF_SEARCH_SLIPS_URL =
    "/circulation-bff/search-slips/{servicePointId}";
  private static final String CIRCULATION_BFF_PICK_SLIPS_URL =
    "/circulation-bff/pick-slips/{servicePointId}";
  private static final String CIRCULATION_SEARCH_SLIPS_URL =
    "/circulation/search-slips";
  private static final String CIRCULATION_PICK_SLIPS_URL =
    "/circulation/pick-slips";
  private static final String TLR_SEARCH_SLIPS_URL = "/tlr/search-slips";
  private static final String TLR_PICK_SLIPS_URL = "/tlr/pick-slips";
  public static final String SERVICE_POINT_ID = UUID.randomUUID().toString();

  public static UserTenantCollection buildUserTenantCollection(String tenantId) {
    var userTenant = new UserTenant();
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    userTenant.setTenantId(tenantId);
    return new UserTenantCollection().addUserTenantsItem(userTenant);
  }

  public static TlrSettings buildTlrSettings(boolean isTlrEnabled) {
    TlrSettings tlrSettings = new TlrSettings();
    tlrSettings.setEcsTlrFeatureEnabled(isTlrEnabled);
    return tlrSettings;
  }

  public static CirculationSettingsResponse buildCirculationTlrSettingsResponse(
    boolean isTlrEnabled) {

    var circulationSettingsResponse = new CirculationSettingsResponse();
    circulationSettingsResponse.setTotalRecords(1);
    circulationSettingsResponse.setCirculationSettings(List.of(
      new CirculationSettings()
        .name("ecsTlrFeature")
        .value(new CirculationSettingsValue().enabled(isTlrEnabled))
    ));

    return circulationSettingsResponse;
  }

  public static StaffSlipCollection buildStaffSlipCollection() {
    return new StaffSlipCollection(1, List.of(new StaffSlip()));
  }

  public static Stream<Arguments> isCentralTenantToIsTlrEnabledToUrlForStaffSLipsToCircBffUrl() {
    return Stream.of(
      Arguments.of(false, false, CIRCULATION_SEARCH_SLIPS_URL,
        CIRCULATION_BFF_SEARCH_SLIPS_URL),
      Arguments.of(true, false, CIRCULATION_SEARCH_SLIPS_URL,
        CIRCULATION_BFF_SEARCH_SLIPS_URL),
      Arguments.of(false, false, CIRCULATION_PICK_SLIPS_URL,
        CIRCULATION_BFF_PICK_SLIPS_URL),
      Arguments.of(true, false, CIRCULATION_PICK_SLIPS_URL,
        CIRCULATION_BFF_PICK_SLIPS_URL),
      Arguments.of(false, true, TLR_SEARCH_SLIPS_URL, CIRCULATION_BFF_SEARCH_SLIPS_URL),
      Arguments.of(true, true, TLR_SEARCH_SLIPS_URL, CIRCULATION_BFF_SEARCH_SLIPS_URL),
      Arguments.of(false, true, TLR_PICK_SLIPS_URL, CIRCULATION_BFF_PICK_SLIPS_URL),
      Arguments.of(true, true, TLR_PICK_SLIPS_URL, CIRCULATION_BFF_PICK_SLIPS_URL)
    );
  }
}

