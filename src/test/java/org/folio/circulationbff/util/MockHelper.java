package org.folio.circulationbff.util;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.apache.http.HttpStatus.SC_OK;
import static org.folio.circulationbff.api.BaseIT.HEADER_TENANT;
import static org.folio.circulationbff.api.BaseIT.TENANT_ID_CONSORTIUM;
import static org.folio.circulationbff.api.BaseIT.asJsonString;
import static org.folio.circulationbff.api.BaseIT.randomId;

import java.util.List;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.AllowedServicePoints1Inner;
import org.folio.circulationbff.domain.dto.CirculationSettings;
import org.folio.circulationbff.domain.dto.CirculationSettingsResponse;
import org.folio.circulationbff.domain.dto.CirculationSettingsValue;
import org.folio.circulationbff.domain.dto.PickSlipCollection;
import org.folio.circulationbff.domain.dto.SearchSlipCollection;
import org.folio.circulationbff.domain.dto.TlrSettings;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.folio.circulationbff.domain.dto.UserTenantCollection;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

public class MockHelper {
  private static final String CIRCULATION_SETTINGS_URL = "/circulation/settings";
  private static final String TLR_SETTINGS_URL = "/tlr/settings";
  private static final String USER_TENANTS_URL = "/user-tenants";
  public static final String TLR_ALLOWED_SERVICE_POINT_URL = "/tlr/allowed-service-points";

  private WireMockServer wireMockServer;

  public MockHelper(WireMockServer wireMockServer) {
    this.wireMockServer = wireMockServer;
  }

  public void mockEcsTlrSettings(boolean enabled) {
    TlrSettings tlrSettings = new TlrSettings().ecsTlrFeatureEnabled(enabled);
    wireMockServer.stubFor(WireMock.get(urlMatching(TLR_SETTINGS_URL))
      .withHeader(HEADER_TENANT, WireMock.equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(asJsonString(tlrSettings), SC_OK)));
  }

  public void mockEcsTlrCirculationSettings(boolean enabled, String tenantId) {
    var circulationSettingsResponse = new CirculationSettingsResponse();
    circulationSettingsResponse.setTotalRecords(1);
    circulationSettingsResponse.setCirculationSettings(List.of(
      new CirculationSettings()
        .name("ecsTlrFeature")
        .value(new CirculationSettingsValue().enabled(enabled))
    ));
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(CIRCULATION_SETTINGS_URL))
      .withQueryParam("query", equalTo("name=ecsTlrFeature"))
      .withHeader(HEADER_TENANT, equalTo(tenantId))
      .willReturn(jsonResponse(asJsonString(circulationSettingsResponse),
        SC_OK)));
  }

  public void mockSearchSlips(SearchSlipCollection searchSlips, UrlPathPattern externalUrl,
    String requesterTenantId) {

    wireMockServer.stubFor(WireMock.get(externalUrl)
      .withHeader(HEADER_TENANT, equalTo(requesterTenantId))
      .willReturn(jsonResponse(searchSlips, HttpStatus.SC_OK)));
  }

  public void mockPickSlips(PickSlipCollection pickSlips, UrlPathPattern externalUrl,
    String requesterTenantId) {

    wireMockServer.stubFor(WireMock.get(externalUrl)
      .withHeader(HEADER_TENANT, equalTo(requesterTenantId))
      .willReturn(jsonResponse(pickSlips, HttpStatus.SC_OK)));
  }

  public void mockUserTenants(UserTenantCollection userTenants, String requesterTenantId) {
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(USER_TENANTS_URL))
      .withHeader(HEADER_TENANT, equalTo(requesterTenantId))
      .withQueryParam("limit", matching("\\d*"))
      .willReturn(jsonResponse(asJsonString(userTenants), SC_OK)));
  }

  public void mockEcsTlrSettings(TlrSettings tlrSettings, String requesterTenantId) {
    wireMockServer.stubFor(WireMock.get(urlMatching(TLR_SETTINGS_URL))
      .withHeader(HEADER_TENANT, equalTo(requesterTenantId))
      .willReturn(jsonResponse(asJsonString(tlrSettings), SC_OK)));
  }

  public void mockUserTenants(UserTenant userTenant, String requestTenant) {
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(USER_TENANTS_URL))
      .withQueryParam("limit", matching("\\d*"))
      .withHeader(HEADER_TENANT, equalTo(requestTenant))
      .willReturn(jsonResponse(asJsonString(new UserTenantCollection().addUserTenantsItem(userTenant)),
        SC_OK)));
  }

  public void mockAllowedServicePoints(String requestTenant) {
    var allowedSpResponseConsortium = new AllowedServicePoints();
    allowedSpResponseConsortium.setHold(Set.of(
      buildAllowedServicePoint("SP_consortium_1"),
      buildAllowedServicePoint("SP_consortium_2")));
    wireMockServer.stubFor(WireMock.get(urlPathEqualTo(TLR_ALLOWED_SERVICE_POINT_URL))
      .withHeader(HEADER_TENANT, equalTo(requestTenant))
      .willReturn(jsonResponse(asJsonString(allowedSpResponseConsortium), SC_OK)));
  }

  public AllowedServicePoints1Inner buildAllowedServicePoint(String name) {
    return new AllowedServicePoints1Inner()
      .id(randomId())
      .name(name);
  }

}
