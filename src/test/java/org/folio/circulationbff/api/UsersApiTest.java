package org.folio.circulationbff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.jsonResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.apache.http.HttpStatus;
import org.folio.circulationbff.domain.dto.User;
import org.folio.circulationbff.domain.dto.UserCollection;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.common.Json;

import lombok.SneakyThrows;

class UsersApiTest extends BaseIT {

  private static final String EXTERNAL_USER_ID_PARAM = "externalUserId";
  private static final String TENANT_ID_PARAM = "tenantId";
  private static final String CIRCULATION_BFF_URL =
    String.format("/circulation-bff/external-users/{%s}/tenant/{%s}",
      EXTERNAL_USER_ID_PARAM, TENANT_ID_PARAM);
  private static final String MOD_USERS_PARAM = "externalSystemId==externalUserId";
  private static final String MOD_USERS_URL = "/users";
  private static final String PARAM_QUERY = "query";
  private static final String MOD_USERS_WITH_PARAM_URL =
    MOD_USERS_URL + "?" + PARAM_QUERY + "=" + MOD_USERS_PARAM;

  @Test
  @SneakyThrows
  void getUsersByQueryTest() {
    UserCollection expected = buildUsers();

    wireMockServer.stubFor(WireMock.get(urlPathMatching(MOD_USERS_URL))
      .withQueryParam(PARAM_QUERY, equalTo(MOD_USERS_PARAM))
      .willReturn(jsonResponse(expected, HttpStatus.SC_OK)));

    mockMvc.perform(
        get(CIRCULATION_BFF_URL, EXTERNAL_USER_ID_PARAM, TENANT_ID_PARAM)
          .headers(defaultHeaders())
          .contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().json(Json.write(expected)));

    wireMockServer.verify(1, getRequestedFor(urlPathMatching(MOD_USERS_URL))
      .withQueryParam(PARAM_QUERY, equalTo(MOD_USERS_PARAM)));
  }

  private static UserCollection buildUsers() {
    UserCollection mockedUsers = new UserCollection();
    User mockedUser = new User();
    mockedUsers.setUsers(List.of(mockedUser));
    return mockedUsers;
  }

}
