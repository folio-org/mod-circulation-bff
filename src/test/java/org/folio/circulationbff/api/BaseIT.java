package org.folio.circulationbff.api;

import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;
import java.util.UUID;

import org.folio.circulationbff.controller.CirculationBffController;
import org.folio.circulationbff.controller.TenantController;
import org.folio.circulationbff.util.WireMockInitializer;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import com.github.tomakehurst.wiremock.WireMockServer;

@ActiveProfiles("test")
@ContextConfiguration(initializers = {WireMockInitializer.class})
@WebMvcTest({CirculationBffController.class, TenantController.class})
public class BaseIT {
  protected static final String TOKEN = "test_token";
  protected static final String TENANT_ID_CONSORTIUM = "consortium";
  protected static final String USER_ID = randomId();

  @Autowired
  protected MockMvc mockMvc;
  @Autowired
  public WireMockServer wireMockServer;

  @BeforeAll
  static void beforeAll() {
  }

  @BeforeEach
  void beforeEachTest() {
    wireMockServer.resetAll();
  }

  @AfterEach
  public void afterEachTest() {
  }

  public static HttpHeaders defaultHeaders() {
    final HttpHeaders httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.put(XOkapiHeaders.TENANT, List.of(TENANT_ID_CONSORTIUM));
    httpHeaders.add(XOkapiHeaders.TOKEN, TOKEN);
    httpHeaders.add(XOkapiHeaders.USER_ID, USER_ID);

    return httpHeaders;
  }

  protected static String randomId() {
    return UUID.randomUUID().toString();
  }

}