package org.folio.circulationbff.api;

import static java.util.stream.Collectors.toMap;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.util.TestSocketUtils.findAvailableTcpPort;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.folio.circulationbff.service.impl.TenantServiceImpl;
import org.folio.circulationbff.util.MockHelper;
import org.folio.circulationbff.util.TestUtils;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.tomakehurst.wiremock.WireMockServer;

import jakarta.servlet.http.Cookie;
import lombok.SneakyThrows;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class BaseIT {
  public static final String HEADER_TENANT = "x-okapi-tenant";
  protected static final String TOKEN = "test_token";
  public static final String TENANT_ID_CONSORTIUM = "consortium";
  public static final String TENANT_ID_COLLEGE = "college";
  public static final String TENANT_ID_SECURE = "secure";
  protected static final String USER_ID = randomId();
  static MockHelper mockHelper;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private FolioExecutionContextSetter contextSetter;

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  private FolioModuleMetadata moduleMetadata;

  protected static WireMockServer wireMockServer = new WireMockServer(findAvailableTcpPort());
  static {
    wireMockServer.start();
    mockHelper = new MockHelper(wireMockServer);
  }

  @BeforeAll
  static void beforeAll(@Autowired MockMvc mockMvc) {
    setUpTenant(mockMvc);
  }

  @BeforeEach
  void beforeEachTest() {
    contextSetter = initFolioContext();
    wireMockServer.resetAll();
    TenantServiceImpl.clearCache();
  }

  @AfterEach
  void afterEachTest() {
    contextSetter.close();
  }

  @DynamicPropertySource
  static void overrideProperties(DynamicPropertyRegistry registry) {
    registry.add("folio.okapi-url", wireMockServer::baseUrl);
    registry.add("folio.tenant.secure-tenant-id", () -> TENANT_ID_SECURE);
  }

  @SneakyThrows
  protected static void setUpTenant(MockMvc mockMvc) {
    mockMvc.perform(post("/_/tenant")
      .content(asJsonString(new TenantAttributes().moduleTo("mod-circulation-bff")))
      .headers(defaultHeaders())
      .contentType(APPLICATION_JSON)).andExpect(status().isNoContent());
  }

  public static HttpHeaders buildHeaders(String tenantId) {
    HttpHeaders headers = defaultHeaders();
    headers.set(XOkapiHeaders.TENANT, tenantId);
    return headers;
  }

  public static HttpHeaders defaultHeaders() {
    final HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setContentType(APPLICATION_JSON);
    httpHeaders.add(XOkapiHeaders.TENANT, TENANT_ID_CONSORTIUM);
    httpHeaders.add(XOkapiHeaders.URL, wireMockServer.baseUrl());
    httpHeaders.add(XOkapiHeaders.TOKEN, TOKEN);
    httpHeaders.add(XOkapiHeaders.USER_ID, USER_ID);

    return httpHeaders;
  }

  protected ResultActions doPostWithTenant(String url, Object payload, String tenantId) throws Exception {
    String token = TestUtils.buildToken(tenantId);
    return doPostWithToken(url, payload, token);
  }

  protected ResultActions doPostWithToken(String url, Object payload, String token) throws Exception {
    MockHttpServletRequestBuilder requestBuilder = buildRequest(MockMvcRequestBuilders.post(url), payload);
    requestBuilder.cookie(new Cookie("folioAccessToken", token));
    return mockMvc.perform(requestBuilder);
  }

  protected MockHttpServletRequestBuilder buildRequest(MockHttpServletRequestBuilder requestBuilder, Object payload) {
    return requestBuilder
      .headers(defaultHeaders())
      .content(asJsonString(payload));
  }

  protected FolioExecutionContextSetter initFolioContext() {
    HashMap<String, Collection<String>> headers = new HashMap<>(defaultHeaders().entrySet()
      .stream()
      .collect(toMap(Map.Entry::getKey, Map.Entry::getValue)));

    return new FolioExecutionContextSetter(moduleMetadata, headers);
  }

  public static String randomId() {
    return UUID.randomUUID().toString();
  }

  @SneakyThrows
  public static String asJsonString(Object value) {
    return OBJECT_MAPPER.writeValueAsString(value);
  }

}
