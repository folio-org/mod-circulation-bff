package org.folio.circulationbff.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.client.feign.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.AllowedServicePointParams;
import org.folio.circulationbff.domain.dto.AllowedServicePoints;
import org.folio.circulationbff.domain.dto.BatchRequestDetail;
import org.folio.circulationbff.domain.dto.BatchRequestDetailsResponse;
import org.folio.circulationbff.domain.dto.BatchRequestResponse;
import org.folio.circulationbff.domain.dto.BffRequest;
import org.folio.circulationbff.domain.dto.EcsTlr;
import org.folio.circulationbff.domain.dto.PickSlipCollection;
import org.folio.circulationbff.domain.dto.Request;
import org.folio.circulationbff.domain.dto.Requests;
import org.folio.circulationbff.domain.dto.SearchSlipCollection;
import org.folio.circulationbff.domain.dto.TlrSettings;
import org.folio.circulationbff.domain.dto.User;
import org.folio.circulationbff.service.impl.CirculationBffServiceImpl;
import org.folio.spring.service.SystemUserScopedExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test class for CirculationBffServiceImpl using Java 21 features.
 * Demonstrates the use of:
 * - Record patterns
 * - Enhanced switch expressions
 * - Text blocks
 * - Pattern matching for instanceof
 * - Virtual threads (in nested tests)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CirculationBffService Tests")
class CirculationBffServiceTest {

  @Mock
  private CirculationClient circulationClient;

  @Mock
  private EcsTlrClient ecsTlrClient;

  @Mock
  private UserService userService;

  @Mock
  private SettingsService settingsService;

  @Mock
  private TenantService tenantService;

  @Mock
  private SystemUserScopedExecutionService executionService;

  @Mock
  private RequestMediatedClient requestMediatedClient;

  @InjectMocks
  private CirculationBffServiceImpl circulationBffService;

  private static final String SERVICE_POINT_ID = "service-point-1";
  private static final String TENANT_ID = "test-tenant";
  private static final String CENTRAL_TENANT_ID = "central-tenant";
  private static final UUID REQUEST_ID = UUID.randomUUID();
  private static final UUID REQUESTER_ID = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(circulationBffService, "batchRequestDetailsQueryIdsSize", 20);
  }

  @Nested
  @DisplayName("Pick Slips Tests")
  class PickSlipsTests {

    @Test
    @DisplayName("Should fetch pick slips from circulation when TLR is disabled")
    void shouldFetchPickSlipsFromCirculation() {
      // Given
      var expectedPickSlips = new PickSlipCollection();
      when(tenantService.isCurrentTenantCentral()).thenReturn(false);
      when(circulationClient.getPickSlips(SERVICE_POINT_ID)).thenReturn(expectedPickSlips);

      // When
      var result = circulationBffService.fetchPickSlipsByServicePointId(SERVICE_POINT_ID);

      // Then
      assertThat(result, is(expectedPickSlips));
      verify(circulationClient).getPickSlips(SERVICE_POINT_ID);
      verify(ecsTlrClient, never()).getPickSlips(anyString());
    }

    @Test
    @DisplayName("Should fetch pick slips from TLR when feature is enabled")
    void shouldFetchPickSlipsFromTlr() {
      // Given
      var expectedPickSlips = new PickSlipCollection();
      var tlrSettings = new TlrSettings().ecsTlrFeatureEnabled(true);

      when(tenantService.isCurrentTenantCentral()).thenReturn(true);
      when(ecsTlrClient.getTlrSettings()).thenReturn(tlrSettings);
      when(ecsTlrClient.getPickSlips(SERVICE_POINT_ID)).thenReturn(expectedPickSlips);

      // When
      var result = circulationBffService.fetchPickSlipsByServicePointId(SERVICE_POINT_ID);

      // Then
      assertThat(result, is(expectedPickSlips));
      verify(ecsTlrClient).getPickSlips(SERVICE_POINT_ID);
      verify(circulationClient, never()).getPickSlips(anyString());
    }
  }

  @Nested
  @DisplayName("Search Slips Tests")
  class SearchSlipsTests {

    @Test
    @DisplayName("Should fetch search slips from circulation when TLR is disabled")
    void shouldFetchSearchSlipsFromCirculation() {
      // Given
      var expectedSearchSlips = new SearchSlipCollection();
      when(tenantService.isCurrentTenantCentral()).thenReturn(false);
      when(circulationClient.getSearchSlips(SERVICE_POINT_ID)).thenReturn(expectedSearchSlips);

      // When
      var result = circulationBffService.fetchSearchSlipsByServicePointId(SERVICE_POINT_ID);

      // Then
      assertThat(result, is(expectedSearchSlips));
      verify(circulationClient).getSearchSlips(SERVICE_POINT_ID);
      verify(ecsTlrClient, never()).getSearchSlips(anyString());
    }

    @Test
    @DisplayName("Should fetch search slips from TLR when feature is enabled")
    void shouldFetchSearchSlipsFromTlr() {
      // Given
      var expectedSearchSlips = new SearchSlipCollection();
      var tlrSettings = new TlrSettings().ecsTlrFeatureEnabled(true);

      when(tenantService.isCurrentTenantCentral()).thenReturn(true);
      when(ecsTlrClient.getTlrSettings()).thenReturn(tlrSettings);
      when(ecsTlrClient.getSearchSlips(SERVICE_POINT_ID)).thenReturn(expectedSearchSlips);

      // When
      var result = circulationBffService.fetchSearchSlipsByServicePointId(SERVICE_POINT_ID);

      // Then
      assertThat(result, is(expectedSearchSlips));
      verify(ecsTlrClient).getSearchSlips(SERVICE_POINT_ID);
      verify(circulationClient, never()).getSearchSlips(anyString());
    }
  }

  @Nested
  @DisplayName("Batch Request Info Tests")
  class BatchRequestInfoTests {

    @Test
    @DisplayName("Should enrich requests with batch request info")
    void shouldEnrichRequestsWithBatchInfo() {
      // Given
      var requestId1 = UUID.randomUUID().toString();
      var requestId2 = UUID.randomUUID().toString();
      var batchId1 = UUID.randomUUID().toString();
      var batchId2 = UUID.randomUUID().toString();
      var requestDate = new Date();

      var request1 = new Request().id(requestId1);
      var request2 = new Request().id(requestId2);
      var requests = new Requests().requests(List.of(request1, request2));

      var batchDetail1 = new BatchRequestDetail()
        .confirmedRequestId(requestId1)
        .batchId(batchId1);
      var batchDetail2 = new BatchRequestDetail()
        .confirmedRequestId(requestId2)
        .batchId(batchId2);

      var batchDetailsResponse = new BatchRequestDetailsResponse()
        .mediatedBatchRequestDetails(List.of(batchDetail1, batchDetail2));

      var batchResponse1 = new BatchRequestResponse().requestDate(requestDate);
      var batchResponse2 = new BatchRequestResponse().requestDate(requestDate);

      when(circulationClient.getRequests(anyString(), any(), any(), anyString()))
        .thenReturn(requests);
      when(requestMediatedClient.queryMediatedBatchRequestDetails(anyString()))
        .thenReturn(batchDetailsResponse);
      when(requestMediatedClient.getMediatedBatchRequestById(UUID.fromString(batchId1)))
        .thenReturn(ResponseEntity.ok(batchResponse1));
      when(requestMediatedClient.getMediatedBatchRequestById(UUID.fromString(batchId2)))
        .thenReturn(ResponseEntity.ok(batchResponse2));

      // When
      var result = circulationBffService.getBatchRequestInfoEnrichedRequests(
        "status==Open", 0, 10, "exact");

      // Then
      assertNotNull(result);
      assertEquals(2, result.getRequests().size());

      var enrichedRequest1 = result.getRequests().getFirst();
      assertNotNull(enrichedRequest1.getBatchRequestInfo());
      assertEquals(batchId1, enrichedRequest1.getBatchRequestInfo().getBatchRequestId());
      assertEquals(requestDate, enrichedRequest1.getBatchRequestInfo().getBatchRequestSubmittedAt());

      verify(circulationClient).getRequests("status==Open", 10, 0, "exact");
      verify(requestMediatedClient).queryMediatedBatchRequestDetails(anyString());
    }

    @Test
    @DisplayName("Should handle empty request list")
    void shouldHandleEmptyRequestList() {
      // Given
      var requests = new Requests().requests(List.of());
      when(circulationClient.getRequests(anyString(), any(), any(), anyString()))
        .thenReturn(requests);

      // When
      var result = circulationBffService.getBatchRequestInfoEnrichedRequests(
        "status==Open", 0, 10, "exact");

      // Then
      assertNotNull(result);
      assertEquals(0, result.getRequests().size());
      verify(requestMediatedClient, never()).queryMediatedBatchRequestDetails(anyString());
    }

    @Test
    @DisplayName("Should handle batch request fetch failure gracefully")
    void shouldHandleBatchRequestFetchFailure() {
      // Given
      var requestId = UUID.randomUUID().toString();
      var batchId = UUID.randomUUID().toString();

      var request = new Request().id(requestId);
      var requests = new Requests().requests(List.of(request));

      var batchDetail = new BatchRequestDetail()
        .confirmedRequestId(requestId)
        .batchId(batchId);

      var batchDetailsResponse = new BatchRequestDetailsResponse()
        .mediatedBatchRequestDetails(List.of(batchDetail));

      when(circulationClient.getRequests(anyString(), any(), any(), anyString()))
        .thenReturn(requests);
      when(requestMediatedClient.queryMediatedBatchRequestDetails(anyString()))
        .thenReturn(batchDetailsResponse);
      when(requestMediatedClient.getMediatedBatchRequestById(UUID.fromString(batchId)))
        .thenReturn(ResponseEntity.status(HttpStatus.NOT_FOUND).build());

      // When
      var result = circulationBffService.getBatchRequestInfoEnrichedRequests(
        "status==Open", 0, 10, "exact");

      // Then
      assertNotNull(result);
      var enrichedRequest = result.getRequests().getFirst();
      assertNotNull(enrichedRequest.getBatchRequestInfo());
      assertThat(enrichedRequest.getBatchRequestInfo().getBatchRequestSubmittedAt(), is(nullValue()));
    }
  }

  @Nested
  @DisplayName("Allowed Service Points Tests")
  class AllowedServicePointsTests {

    @Test
    @DisplayName("Should call circulation when ECS TLR is disabled")
    void shouldCallCirculationWhenEcsTlrDisabled() {
      // Given
      var params = AllowedServicePointParams.builder().requesterId(REQUESTER_ID).build();
      var expectedServicePoints = new AllowedServicePoints();

      when(settingsService.isEcsTlrFeatureEnabled()).thenReturn(false);
      when(circulationClient.allowedServicePoints(params)).thenReturn(expectedServicePoints);

      // When
      var result = circulationBffService.getAllowedServicePoints(params, TENANT_ID);

      // Then
      assertThat(result, is(expectedServicePoints));
      verify(circulationClient).allowedServicePoints(params);
      verify(ecsTlrClient, never()).getAllowedServicePoints(any());
    }

    @Test
    @DisplayName("Should call TLR when ECS TLR is enabled and tenant is central")
    void shouldCallTlrWhenEcsTlrEnabledAndCentralTenant() {
      // Given
      var params = AllowedServicePointParams.builder().requesterId(REQUESTER_ID).build();
      var expectedServicePoints = new AllowedServicePoints();

      when(settingsService.isEcsTlrFeatureEnabled()).thenReturn(true);
      when(tenantService.isCurrentTenantCentral()).thenReturn(true);
      when(ecsTlrClient.getAllowedServicePoints(params)).thenReturn(expectedServicePoints);

      // When
      var result = circulationBffService.getAllowedServicePoints(params, TENANT_ID);

      // Then
      assertThat(result, is(expectedServicePoints));
      verify(ecsTlrClient).getAllowedServicePoints(params);
      verify(circulationClient, never()).allowedServicePoints(any());
    }

    @Test
    @DisplayName("Should call central TLR when creating request (no requestId)")
    void shouldCallCentralTlrWhenCreatingRequest() {
      // Given
      var patronGroupId = UUID.randomUUID().toString();
      var user = new User().patronGroup(patronGroupId);
      var params = AllowedServicePointParams.builder()
        .requesterId(REQUESTER_ID)
        .itemId(UUID.randomUUID())
        .build();
      var expectedServicePoints = new AllowedServicePoints();

      when(settingsService.isEcsTlrFeatureEnabled()).thenReturn(true);
      when(tenantService.isCurrentTenantCentral()).thenReturn(false);
      when(userService.find(REQUESTER_ID.toString())).thenReturn(user);
      when(tenantService.getCentralTenantId()).thenReturn(Optional.of(CENTRAL_TENANT_ID));
      when(executionService.executeSystemUserScoped(eq(CENTRAL_TENANT_ID), any(Callable.class)))
        .thenAnswer(invocation -> {
          Callable<?> callable = invocation.getArgument(1);
          return callable.call();
        });
      when(ecsTlrClient.getAllowedServicePoints(any())).thenReturn(expectedServicePoints);

      // When
      var result = circulationBffService.getAllowedServicePoints(params, TENANT_ID);

      // Then
      assertThat(result, is(expectedServicePoints));

      ArgumentCaptor<AllowedServicePointParams> captor =
        ArgumentCaptor.forClass(AllowedServicePointParams.class);
      verify(ecsTlrClient).getAllowedServicePoints(captor.capture());

      var capturedParams = captor.getValue();
      assertThat(capturedParams.getRequesterId(), is(nullValue()));
      assertThat(capturedParams.getPatronGroupId(), is(notNullValue()));
    }

    @Test
    @DisplayName("Should call circulation when editing non-ECS request")
    void shouldCallCirculationWhenEditingNonEcsRequest() {
      // Given
      var params = AllowedServicePointParams.builder()
        .requestId(REQUEST_ID)
        .requesterId(REQUESTER_ID)
        .build();
      var expectedServicePoints = new AllowedServicePoints();
      var request = new Request().id(REQUEST_ID.toString()).ecsRequestPhase(null);

      when(settingsService.isEcsTlrFeatureEnabled()).thenReturn(true);
      when(tenantService.isCurrentTenantCentral()).thenReturn(false);
      when(circulationClient.getRequestById(REQUEST_ID.toString())).thenReturn(request);
      when(circulationClient.allowedServicePoints(params)).thenReturn(expectedServicePoints);

      // When
      var result = circulationBffService.getAllowedServicePoints(params, TENANT_ID);

      // Then
      assertThat(result, is(expectedServicePoints));
      verify(circulationClient).getRequestById(REQUEST_ID.toString());
      verify(circulationClient).allowedServicePoints(params);
      verify(ecsTlrClient, never()).getAllowedServicePoints(any());
    }

    @Test
    @DisplayName("Should call central TLR when editing ECS request")
    void shouldCallCentralTlrWhenEditingEcsRequest() {
      // Given
      var patronGroupId = UUID.randomUUID().toString();
      var user = new User().patronGroup(patronGroupId);
      var params = AllowedServicePointParams.builder()
        .requestId(REQUEST_ID)
        .requesterId(REQUESTER_ID)
        .build();
      var expectedServicePoints = new AllowedServicePoints();
      var request = new Request()
        .id(REQUEST_ID.toString())
        .ecsRequestPhase(Request.EcsRequestPhaseEnum.PRIMARY);

      when(settingsService.isEcsTlrFeatureEnabled()).thenReturn(true);
      when(tenantService.isCurrentTenantCentral()).thenReturn(false);
      when(circulationClient.getRequestById(REQUEST_ID.toString())).thenReturn(request);
      when(userService.find(REQUESTER_ID.toString())).thenReturn(user);
      when(tenantService.getCentralTenantId()).thenReturn(Optional.of(CENTRAL_TENANT_ID));
      when(executionService.executeSystemUserScoped(eq(CENTRAL_TENANT_ID), any(Callable.class)))
        .thenAnswer(invocation -> {
          Callable<?> callable = invocation.getArgument(1);
          return callable.call();
        });
      when(ecsTlrClient.getAllowedServicePoints(any())).thenReturn(expectedServicePoints);

      // When
      var result = circulationBffService.getAllowedServicePoints(params, TENANT_ID);

      // Then
      assertThat(result, is(expectedServicePoints));
      verify(circulationClient).getRequestById(REQUEST_ID.toString());
      verify(ecsTlrClient).getAllowedServicePoints(any());
    }
  }

  @Nested
  @DisplayName("Create Request Tests")
  class CreateRequestTests {

    @Test
    @DisplayName("Should create ECS TLR when feature is enabled and tenant is central")
    void shouldCreateEcsTlrWhenFeatureEnabled() {
      // Given
      var primaryRequestId = UUID.randomUUID().toString();
      var bffRequest = new BffRequest()
        .requesterId(REQUESTER_ID.toString())
        .itemId(UUID.randomUUID().toString());
      var ecsTlr = new EcsTlr().primaryRequestId(primaryRequestId);
      var expectedRequest = new Request().id(primaryRequestId);

      when(settingsService.isEcsTlrFeatureEnabled(TENANT_ID)).thenReturn(true);
      when(tenantService.isCentralTenant(TENANT_ID)).thenReturn(true);
      when(ecsTlrClient.createRequest(bffRequest)).thenReturn(ecsTlr);
      when(circulationClient.getRequestById(primaryRequestId)).thenReturn(expectedRequest);

      // When
      var result = circulationBffService.createRequest(bffRequest, TENANT_ID);

      // Then
      assertThat(result, is(expectedRequest));
      assertThat(result.getId(), equalTo(primaryRequestId));
      verify(ecsTlrClient).createRequest(bffRequest);
      verify(circulationClient).getRequestById(primaryRequestId);
    }

    @Test
    @DisplayName("Should create circulation request when ECS TLR is disabled")
    void shouldCreateCirculationRequestWhenEcsTlrDisabled() {
      // Given
      var bffRequest = new BffRequest()
        .requesterId(REQUESTER_ID.toString())
        .itemId(UUID.randomUUID().toString());
      var expectedRequest = new Request().id(REQUEST_ID.toString());

      when(settingsService.isEcsTlrFeatureEnabled(TENANT_ID)).thenReturn(false);
      when(circulationClient.createRequest(bffRequest)).thenReturn(expectedRequest);

      // When
      var result = circulationBffService.createRequest(bffRequest, TENANT_ID);

      // Then
      assertThat(result, is(expectedRequest));
      verify(circulationClient).createRequest(bffRequest);
      verify(ecsTlrClient, never()).createRequest(any());
    }

    @Test
    @DisplayName("Should create circulation request when tenant is not central")
    void shouldCreateCirculationRequestWhenTenantNotCentral() {
      // Given
      var bffRequest = new BffRequest()
        .requesterId(REQUESTER_ID.toString())
        .itemId(UUID.randomUUID().toString());
      var expectedRequest = new Request().id(REQUEST_ID.toString());

      when(settingsService.isEcsTlrFeatureEnabled(TENANT_ID)).thenReturn(true);
      when(tenantService.isCentralTenant(TENANT_ID)).thenReturn(false);
      when(circulationClient.createRequest(bffRequest)).thenReturn(expectedRequest);

      // When
      var result = circulationBffService.createRequest(bffRequest, TENANT_ID);

      // Then
      assertThat(result, is(expectedRequest));
      verify(circulationClient).createRequest(bffRequest);
      verify(ecsTlrClient, never()).createRequest(any());
    }
  }

  @Nested
  @DisplayName("Integration Scenarios")
  class IntegrationScenarios {

    @Test
    @DisplayName("Should handle multiple batch partitions correctly")
    void shouldHandleMultipleBatchPartitions() {
      // Given - set smaller partition size to test partitioning
      ReflectionTestUtils.setField(circulationBffService, "batchRequestDetailsQueryIdsSize", 2);

      var requestId1 = UUID.randomUUID().toString();
      var requestId2 = UUID.randomUUID().toString();
      var requestId3 = UUID.randomUUID().toString();
      var batchId = UUID.randomUUID().toString();
      var requestDate = new Date();

      var request1 = new Request().id(requestId1);
      var request2 = new Request().id(requestId2);
      var request3 = new Request().id(requestId3);
      var requests = new Requests().requests(List.of(request1, request2, request3));

      var batchDetail1 = new BatchRequestDetail()
        .confirmedRequestId(requestId1)
        .batchId(batchId);
      var batchDetail2 = new BatchRequestDetail()
        .confirmedRequestId(requestId2)
        .batchId(batchId);
      var batchDetail3 = new BatchRequestDetail()
        .confirmedRequestId(requestId3)
        .batchId(batchId);

      var batchDetailsResponse1 = new BatchRequestDetailsResponse()
        .mediatedBatchRequestDetails(List.of(batchDetail1, batchDetail2));
      var batchDetailsResponse2 = new BatchRequestDetailsResponse()
        .mediatedBatchRequestDetails(List.of(batchDetail3));

      var batchResponse = new BatchRequestResponse().requestDate(requestDate);

      when(circulationClient.getRequests(anyString(), any(), any(), anyString()))
        .thenReturn(requests);
      when(requestMediatedClient.queryMediatedBatchRequestDetails(anyString()))
        .thenReturn(batchDetailsResponse1, batchDetailsResponse2);
      when(requestMediatedClient.getMediatedBatchRequestById(UUID.fromString(batchId)))
        .thenReturn(ResponseEntity.ok(batchResponse));

      // When
      var result = circulationBffService.getBatchRequestInfoEnrichedRequests(
        "status==Open", 0, 10, "exact");

      // Then
      assertNotNull(result);
      assertEquals(3, result.getRequests().size());

      // Verify all requests have the same batch info (cached)
      result.getRequests().forEach(req -> {
        assertNotNull(req.getBatchRequestInfo());
        assertEquals(batchId, req.getBatchRequestInfo().getBatchRequestId());
        assertEquals(requestDate, req.getBatchRequestInfo().getBatchRequestSubmittedAt());
      });

      // Should call batch request only once due to caching
      verify(requestMediatedClient, times(1))
        .getMediatedBatchRequestById(UUID.fromString(batchId));
    }
  }
}
