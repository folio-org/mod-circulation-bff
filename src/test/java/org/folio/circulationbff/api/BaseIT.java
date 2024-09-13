package org.folio.circulationbff.api;

import static java.util.stream.Collectors.toMap;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.folio.circulationbff.util.TestUtils;
import org.folio.circulationbff.util.WireMockInitializer;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.folio.tenant.domain.dto.TenantAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.TestSocketUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.tomakehurst.wiremock.WireMockServer;

import lombok.SneakyThrows;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {WireMockInitializer.class})
@Testcontainers
@AutoConfigureMockMvc
public class BaseIT {
  protected static final String HEADER_TENANT = "x-okapi-tenant";
  protected static final String TOKEN = "test_token";
  protected static final String TENANT_ID_CONSORTIUM = "consortium";
  protected static final String USER_ID = randomId();
  private FolioExecutionContextSetter contextSetter;
  private static final int WIRE_MOCK_PORT = TestSocketUtils.findAvailableTcpPort();
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @Autowired
  protected MockMvc mockMvc;
  @Autowired
  public static WireMockServer wireMockServer;
  @Autowired
  private WebTestClient webClient;
  @Autowired
  private FolioModuleMetadata moduleMetadata;

  @BeforeAll
  static void beforeAll() {
    wireMockServer = new WireMockServer(WIRE_MOCK_PORT);
    wireMockServer.start();
  }

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("folio.okapi-url", wireMockServer::baseUrl);
  }

  @BeforeEach
  void beforeEachTest() {
    doPost("/_/tenant", asJsonString(new TenantAttributes().moduleTo("mod-circulation-bff")))
      .expectStatus().isNoContent();

    contextSetter = initFolioContext();
    wireMockServer.resetAll();
  }

  @AfterEach
  public void afterEachTest() {
    contextSetter.close();
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
  protected WebTestClient.ResponseSpec doPost(String url, Object payload) {
    return doPostWithTenant(url, payload, TENANT_ID_CONSORTIUM);
  }

  protected WebTestClient.ResponseSpec doPostWithTenant(String url, Object payload, String tenantId) {
    return doPostWithToken(url, payload, TestUtils.buildToken(tenantId));
  }

  protected WebTestClient.ResponseSpec doPostWithToken(String url, Object payload, String token) {
    return buildRequest(HttpMethod.POST, url)
      .cookie("folioAccessToken", token)
      .body(BodyInserters.fromValue(payload))
      .exchange();
  }

  protected WebTestClient.RequestBodySpec buildRequest(HttpMethod method, String uri) {
    return webClient.method(method)
      .uri(uri)
      .accept(APPLICATION_JSON)
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID_CONSORTIUM)
      .header(XOkapiHeaders.URL, wireMockServer.baseUrl())
      .header(XOkapiHeaders.TOKEN, TOKEN)
      .header(XOkapiHeaders.USER_ID, randomId());
  }

  @SneakyThrows
  public static String asJsonString(Object value) {
    return OBJECT_MAPPER.writeValueAsString(value);
  }

  protected FolioExecutionContextSetter initFolioContext() {
    return new FolioExecutionContextSetter(moduleMetadata, buildDefaultHeaders());
  }

  private static Map<String, Collection<String>> buildDefaultHeaders() {
    return new HashMap<>(defaultHeaders().entrySet()
      .stream()
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  protected WebTestClient.ResponseSpec doGet(String url) {
    return buildRequest(HttpMethod.GET, url)
      .exchange();
  }
}
