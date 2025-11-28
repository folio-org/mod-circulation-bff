package org.folio.circulationbff.util;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.util.Base64;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

import com.github.tomakehurst.wiremock.WireMockServer;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestUtils {

  @SneakyThrows
  public static String buildToken(String tenantId) {
    JSONObject header = new JSONObject()
      .put("alg", "HS256");

    JSONObject payload = new JSONObject()
      .put("sub", tenantId + "_admin")
      .put("user_id", "bb6a6f19-9275-4261-ad9d-6c178c24c4fb")
      .put("type", "access")
      .put("exp", 1708342543)
      .put("iat", 1708341943)
      .put("tenant", tenantId);

    String signature = "De_0um7P_Rv-diqjHKLcSHZdjzjjshvlBbi6QPrz0Tw";

    return String.format("%s.%s.%s",
      Base64.getEncoder().encodeToString(header.toString().getBytes()),
      Base64.getEncoder().encodeToString(payload.toString().getBytes()),
      signature);
  }

  @SneakyThrows
  public static void mockUserTenants(WireMockServer wireMockServer, String tenantId,
    UUID consortiumId) {

    wireMockServer.stubFor(get(urlEqualTo("/user-tenants?limit=1"))
      .willReturn(okJson(new JSONObject()
        .put("totalRecords", 1)
        .put("userTenants", new JSONArray()
          .put(new JSONObject()
            .put("centralTenantId", "consortium")
            .put("consortiumId", consortiumId)
            .put("userId", UUID.randomUUID().toString())
            .put("tenantId", tenantId)))
        .toString())));
  }

  public static String randomId() {
    return UUID.randomUUID().toString();
  }
}
