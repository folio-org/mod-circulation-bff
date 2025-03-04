package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.folio.circulationbff.api.StaffSlipsApiTestDataProvider.CIRCULATION_BFF_PICK_SLIPS_URL;
import static org.folio.circulationbff.api.StaffSlipsApiTestDataProvider.CIRCULATION_BFF_SEARCH_SLIPS_URL;
import static org.folio.circulationbff.api.StaffSlipsApiTestDataProvider.SERVICE_POINT_ID;
import static org.folio.circulationbff.api.StaffSlipsApiTestDataProvider.buildTlrSettings;
import static org.folio.circulationbff.api.StaffSlipsApiTestDataProvider.buildUserTenantCollection;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.stream.Stream;

import org.folio.circulationbff.domain.dto.PickSlipCollection;
import org.folio.circulationbff.domain.dto.SearchSlipCollection;
import org.folio.circulationbff.domain.dto.StaffSlip;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.github.tomakehurst.wiremock.common.Json;

import lombok.SneakyThrows;

class StaffSlipApiTest extends BaseIT {

  private static final String URL_PATTERN = "%s/%s";

  private static Stream<Arguments> searchSlipsTestData() {
    return StaffSlipsApiTestDataProvider.searchSlipsTestData();
  }

  private static Stream<Arguments> pickSlipsTestData() {
    return StaffSlipsApiTestDataProvider.pickSlipsTestData();
  }

  @ParameterizedTest()
  @MethodSource("searchSlipsTestData")
  @SneakyThrows
  void getSearchSlipsApiTest(boolean isCentralTenant, boolean isTlrEnabled,
    String externalModuleUrl) {

    var tenantId = isCentralTenant ? TENANT_ID_CONSORTIUM : TENANT_ID_COLLEGE;
    var searchSlips = new SearchSlipCollection(1, List.of(new StaffSlip()));
    var externalModuleUrlPattern = urlPathMatching(String.format(URL_PATTERN,
      externalModuleUrl, SERVICE_POINT_ID));

    mockHelper.mockUserTenants(buildUserTenantCollection(tenantId), tenantId);
    mockHelper.mockEcsTlrSettings(buildTlrSettings(isTlrEnabled), tenantId);
    mockHelper.mockSearchSlips(searchSlips, externalModuleUrlPattern, tenantId);
    mockSearchSlipsPerform(searchSlips, tenantId);

    wireMockServer.verify(1, getRequestedFor(externalModuleUrlPattern));
  }

  @ParameterizedTest()
  @MethodSource("pickSlipsTestData")
  @SneakyThrows
  void getPickSlipsApiTest(boolean isCentralTenant, boolean isTlrEnabled,
    String externalModuleUrl) {

    var tenantId = isCentralTenant ? TENANT_ID_CONSORTIUM : TENANT_ID_COLLEGE;
    var pickSlips = new PickSlipCollection(1, List.of(new StaffSlip()));
    var externalModuleUrlPattern = urlPathMatching(String.format(URL_PATTERN,
      externalModuleUrl, SERVICE_POINT_ID));

    mockHelper.mockUserTenants(buildUserTenantCollection(tenantId), tenantId);
    mockHelper.mockEcsTlrSettings(buildTlrSettings(isTlrEnabled), tenantId);
    mockHelper.mockPickSlips(pickSlips, externalModuleUrlPattern, tenantId);
    mockPickSlipsPerform(pickSlips, tenantId);

    wireMockServer.verify(1, getRequestedFor(externalModuleUrlPattern));
  }

  @SneakyThrows
  private void mockSearchSlipsPerform(SearchSlipCollection searchSlips, String tenantId) {
    HttpHeaders httpHeaders = defaultHeaders();
    httpHeaders.set(XOkapiHeaders.TENANT, tenantId);
    mockMvc.perform(get(CIRCULATION_BFF_SEARCH_SLIPS_URL, SERVICE_POINT_ID)
        .headers(httpHeaders)
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(Json.write(searchSlips)));
  }

  @SneakyThrows
  private void mockPickSlipsPerform(PickSlipCollection pickSlips, String tenantId) {
    HttpHeaders httpHeaders = defaultHeaders();
    httpHeaders.set(XOkapiHeaders.TENANT, tenantId);
    mockMvc.perform(get(CIRCULATION_BFF_PICK_SLIPS_URL, SERVICE_POINT_ID)
        .headers(httpHeaders)
        .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(Json.write(pickSlips)));
  }

}
