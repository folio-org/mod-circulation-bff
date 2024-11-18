package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.apache.http.HttpStatus.SC_OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.http.HttpStatus;
import org.folio.circulationbff.domain.dto.Slip;
import org.folio.circulationbff.domain.dto.SlipsCollection;
import org.folio.circulationbff.domain.dto.TlrSettings;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;

import lombok.SneakyThrows;

class StaffSlipApiTest extends BaseIT{
  private static final String CIRCULATION_BFF_SEARCH_SLIPS_URL =
    "/circulation-bff/search-slips/{servicePointId}";
  private static final String CIRCULATION_BFF_PICK_SLIPS_URL =
    "/circulation-bff/pick-slips/{servicePointId}";
  private static final String TLR_SETTINGS_URL = "/tlr/settings";
  private static final String CIRCULATION_SEARCH_SLIPS_URL =
    "/circulation/search-slips";
  private static final String CIRCULATION_PICK_SLIPS_URL =
    "/circulation/pick-slips";
  private static final String TLR_SEARCH_SLIPS_URL = "/tlr/search-slips";
  private static final String TLR_PICK_SLIPS_URL = "/tlr/pick-slips";
  private static final String URL_PATTERN = "%s/%s";

  @ParameterizedTest()
  @MethodSource("urlToEcsTlrFeatureEnabled")
  @SneakyThrows
  void getStaffSlipsApiTest(String externalModuleUrl, String circulationBffUrl,
    boolean isTlrEnabled) {

    var tlrSettings = new TlrSettings();
    tlrSettings.setEcsTlrFeatureEnabled(isTlrEnabled);
    var staffSlipsCollection = new SlipsCollection(1, List.of(new Slip()));
    var servicePointId = UUID.randomUUID().toString();
    UrlPathPattern externalModuleUrlPattern = urlPathMatching(String.format(URL_PATTERN,
      externalModuleUrl, servicePointId));

    wireMockServer.stubFor(WireMock.get(externalModuleUrlPattern)
      .willReturn(jsonResponse(staffSlipsCollection, HttpStatus.SC_OK)));

    wireMockServer.stubFor(WireMock.get(urlMatching(TLR_SETTINGS_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(jsonResponse(asJsonString(tlrSettings), SC_OK)));

    mockMvc.perform(get(circulationBffUrl, servicePointId)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(Json.write(staffSlipsCollection)));

    wireMockServer.verify(1, getRequestedFor(externalModuleUrlPattern));
  }

  private static Stream<Arguments> urlToEcsTlrFeatureEnabled() {
    return Stream.of(
      Arguments.of(CIRCULATION_SEARCH_SLIPS_URL, CIRCULATION_BFF_SEARCH_SLIPS_URL, false),
      Arguments.of(CIRCULATION_PICK_SLIPS_URL, CIRCULATION_BFF_PICK_SLIPS_URL, false),
      Arguments.of(TLR_SEARCH_SLIPS_URL, CIRCULATION_BFF_SEARCH_SLIPS_URL, true),
      Arguments.of(TLR_PICK_SLIPS_URL, CIRCULATION_BFF_PICK_SLIPS_URL, true)
    );
  }
}
