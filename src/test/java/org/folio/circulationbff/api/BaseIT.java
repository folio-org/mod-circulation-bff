package org.folio.circulationbff.api;

import static java.util.stream.Collectors.toMap;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.github.tomakehurst.wiremock.WireMockServer;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class BaseIT {
  protected static final String TOKEN = "test_token";
  protected static final String TENANT_ID_CONSORTIUM = "consortium";
  protected static final String USER_ID = randomId();

  @Autowired
  protected MockMvc mockMvc;
  @Autowired
  private FolioModuleMetadata moduleMetadata;

  protected static WireMockServer wireMockServer = new WireMockServer(findAvailableTcpPort());
  static {
    wireMockServer.start();
  }

  @BeforeAll
  static void beforeAll() {
  }

  @BeforeEach
  void beforeEachTest() {
    initFolioContext();
    wireMockServer.resetAll();
  }

  @AfterEach
  public void afterEachTest() {
  }

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("folio.okapi-url", wireMockServer::baseUrl);
  }

  public static HttpHeaders defaultHeaders() {
    final HttpHeaders httpHeaders = new HttpHeaders();

    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.add(XOkapiHeaders.TENANT, TENANT_ID_CONSORTIUM);
    httpHeaders.add(XOkapiHeaders.URL, (wireMockServer.baseUrl()));
    httpHeaders.add(XOkapiHeaders.TOKEN, TOKEN);
    httpHeaders.add(XOkapiHeaders.USER_ID, USER_ID);

    return httpHeaders;
  }

  protected FolioExecutionContextSetter initFolioContext() {
    HashMap<String, Collection<String>> headers = new HashMap<>(defaultHeaders().entrySet()
      .stream()
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));

    return new FolioExecutionContextSetter(moduleMetadata, headers);
  }

  protected static String randomId() {
    return UUID.randomUUID().toString();
  }

}