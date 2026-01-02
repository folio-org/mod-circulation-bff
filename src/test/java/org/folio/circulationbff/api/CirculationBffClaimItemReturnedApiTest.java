package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;

import org.folio.circulationbff.domain.dto.ClaimItemReturnedRequest;
import org.folio.circulationbff.domain.dto.UserTenant;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;

import lombok.SneakyThrows;

class CirculationBffClaimItemReturnedApiTest extends BaseIT {

  private static final String CLAIM_ITEM_RETURNED_URL = "/circulation-bff/loans/%s/claim-item-returned";
  private static final String CIRCULATION_CLAIM_ITEM_RETURNED_URL = "/circulation/loans/%s/claim-item-returned";
  private static final String TLR_CLAIM_ITEM_RETURNED_URL = "/tlr/loans/claim-item-returned";
  private static final String REQUESTS_MEDIATED_CLAIM_ITEM_RETURNED_URL = "/requests-mediated/loans/%s/claim-item-returned";


  @SneakyThrows
  @Test
  void callsCirculationWhenEcsTlrDisabled() {
    var loanId = randomId();
    var userTenant = new UserTenant(randomId(), TENANT_ID_COLLEGE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockHelper.mockUserTenants(userTenant, TENANT_ID_COLLEGE);
    mockHelper.mockEcsTlrCirculationSettings(false, TENANT_ID_COLLEGE);

    wireMockServer.stubFor(WireMock.post(urlPathEqualTo(String.format(CIRCULATION_CLAIM_ITEM_RETURNED_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(noContent()));

    var request = new ClaimItemReturnedRequest()
      .itemClaimedReturnedDateTime(new Date())
      .comment("Returned at desk");

    mockMvc.perform(post(String.format(CLAIM_ITEM_RETURNED_URL, loanId))
        .headers(buildHeaders(TENANT_ID_COLLEGE))
        .contentType(APPLICATION_JSON)
        .content(asJsonString(request)))
      .andExpect(status().isNoContent())
      .andExpect(content().string(""));

    wireMockServer.verify(postRequestedFor(urlPathEqualTo(String.format(CIRCULATION_CLAIM_ITEM_RETURNED_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .withRequestBody(containing("Returned at desk")));
  }

  @Test
  void callsTlrWhenEcsTlrEnabledInCentralTenant() {
    var loanId = randomId();
    var userTenant = new UserTenant(randomId(), TENANT_ID_CONSORTIUM);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockHelper.mockUserTenants(userTenant, TENANT_ID_CONSORTIUM);
    mockHelper.mockEcsTlrSettings(true);

    wireMockServer.stubFor(WireMock.post(urlPathEqualTo(TLR_CLAIM_ITEM_RETURNED_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM))
      .willReturn(noContent()));

    performClaimItemReturned(TENANT_ID_CONSORTIUM, loanId, new ClaimItemReturnedRequest()
      .itemClaimedReturnedDateTime(new Date())
      .comment("Returned at desk"));

    wireMockServer.verify(postRequestedFor(urlPathEqualTo(TLR_CLAIM_ITEM_RETURNED_URL))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_CONSORTIUM)));
  }

  @Test
  void callsRequestMediatedWhenEcsTlrEnabledAndCurrentTenantSecure() {
    var loanId = randomId();
    var userTenant = new UserTenant(randomId(), TENANT_ID_SECURE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockHelper.mockUserTenants(userTenant, TENANT_ID_SECURE);
    mockHelper.mockEcsTlrCirculationSettings(true, TENANT_ID_SECURE);

    wireMockServer.stubFor(WireMock.post(urlPathEqualTo(String.format(REQUESTS_MEDIATED_CLAIM_ITEM_RETURNED_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_SECURE))
      .willReturn(noContent()));

    performClaimItemReturned(TENANT_ID_SECURE, loanId, new ClaimItemReturnedRequest()
      .itemClaimedReturnedDateTime(new Date())
      .comment("Returned at desk"));

    wireMockServer.verify(postRequestedFor(urlPathEqualTo(String.format(REQUESTS_MEDIATED_CLAIM_ITEM_RETURNED_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_SECURE)));
  }

  @Test
  void fallbackToCirculationWhenEcsTlrEnabledButNotCentralOrSecure() {
    var loanId = randomId();
    var userTenant = new UserTenant(randomId(), TENANT_ID_COLLEGE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockHelper.mockUserTenants(userTenant, TENANT_ID_COLLEGE);
    mockHelper.mockEcsTlrCirculationSettings(true, TENANT_ID_COLLEGE);

    wireMockServer.stubFor(WireMock.post(urlPathEqualTo(String.format(CIRCULATION_CLAIM_ITEM_RETURNED_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(noContent()));

    performClaimItemReturned(TENANT_ID_COLLEGE, loanId, new ClaimItemReturnedRequest()
      .itemClaimedReturnedDateTime(new Date())
      .comment("Returned at desk"));

    wireMockServer.verify(postRequestedFor(urlPathEqualTo(String.format(CIRCULATION_CLAIM_ITEM_RETURNED_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE)));
  }

  @SneakyThrows
  @Test
  void validationFailsForMissingRequiredFields() {
    var loanId = randomId();
    var userTenant = new UserTenant(randomId(), TENANT_ID_COLLEGE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockHelper.mockUserTenants(userTenant, TENANT_ID_COLLEGE);
    mockHelper.mockEcsTlrCirculationSettings(false, TENANT_ID_COLLEGE);

    var invalidRequest = new ClaimItemReturnedRequest();

    mockMvc.perform(post(String.format(CLAIM_ITEM_RETURNED_URL, loanId))
        .headers(buildHeaders(TENANT_ID_COLLEGE))
        .contentType(APPLICATION_JSON)
        .content(asJsonString(invalidRequest)))
      .andExpect(status().isBadRequest());
  }

  @Test
  void downstreamServiceErrorIsPropagated() throws Exception {
    var loanId = randomId();
    var userTenant = new UserTenant(randomId(), TENANT_ID_COLLEGE);
    userTenant.setCentralTenantId(TENANT_ID_CONSORTIUM);
    mockHelper.mockUserTenants(userTenant, TENANT_ID_COLLEGE);
    mockHelper.mockEcsTlrCirculationSettings(false, TENANT_ID_COLLEGE);

    wireMockServer.stubFor(WireMock.post(urlPathEqualTo(String.format(CIRCULATION_CLAIM_ITEM_RETURNED_URL, loanId)))
      .withHeader(HEADER_TENANT, equalTo(TENANT_ID_COLLEGE))
      .willReturn(serverError().withBody("Downstream error")));

    var request = new ClaimItemReturnedRequest()
      .itemClaimedReturnedDateTime(new Date())
      .comment("Returned at desk");

    mockMvc.perform(post(String.format(CLAIM_ITEM_RETURNED_URL, loanId))
        .headers(buildHeaders(TENANT_ID_COLLEGE))
        .contentType(APPLICATION_JSON)
        .content(asJsonString(request)))
      .andExpect(status().isInternalServerError())
      .andExpect(content().string("Downstream error"));
  }

  @SneakyThrows
  private void performClaimItemReturned(String tenantId, String loanId, ClaimItemReturnedRequest request) {
    mockMvc.perform(post(String.format(CLAIM_ITEM_RETURNED_URL, loanId))
        .headers(buildHeaders(tenantId))
        .contentType(APPLICATION_JSON)
        .content(asJsonString(request)))
      .andExpect(status().isNoContent());
  }
}
