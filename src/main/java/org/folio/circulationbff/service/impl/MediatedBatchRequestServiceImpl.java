package org.folio.circulationbff.service.impl;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.folio.circulationbff.client.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.BatchRequest;
import org.folio.circulationbff.domain.dto.BatchRequestCollectionResponse;
import org.folio.circulationbff.domain.dto.BatchRequestDetail;
import org.folio.circulationbff.domain.dto.BatchRequestDetailsResponse;
import org.folio.circulationbff.domain.dto.BatchRequestResponse;
import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.folio.circulationbff.service.MediatedBatchRequestService;
import org.folio.circulationbff.service.TenantService;
import org.folio.circulationbff.support.CqlQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class MediatedBatchRequestServiceImpl implements MediatedBatchRequestService {

  private final RequestMediatedClient requestMediatedClient;
  private final TenantService tenantService;

  @Value("${folio.batch-requests.query-request-ids-count}")
  private Integer batchRequestDetailsQueryIdsSize;

  @Override
  public ResponseEntity<BatchRequestResponse> createMediatedBatchRequest(BatchRequest batchRequest) {
    log.debug("createMediatedBatchRequest:: parameters batchRequest: {}", batchRequest);

    return requestMediatedClient.postMediatedBatchRequest(batchRequest);
  }

  @Override
  public ResponseEntity<BatchRequestResponse> retrieveMediatedBatchRequestById(UUID batchRequestId) {
    return requestMediatedClient.getMediatedBatchRequestById(batchRequestId);
  }

  @Override
  public BatchRequestCollectionResponse retrieveMediatedBatchRequestsByQuery(String query, Integer offset, Integer limit) {
    return requestMediatedClient.getMediatedBatchRequestsByQuery(query, limit, offset);
  }

  @Override
  public BatchRequestDetailsResponse retrieveMediatedBatchRequestDetails(UUID batchRequestId, Integer offset, Integer limit) {
    var batchDetails = requestMediatedClient.getMediatedBatchRequestDetails(batchRequestId, limit, offset);
    if (batchDetails != null && isNotEmpty(batchDetails.getMediatedBatchRequestDetails()) && tenantService.isCurrentTenantSecure()) {
      // batch requests created with secure tenant will not have circulation request id in confirmedRequestId field, but it would be
      // mediated request id in that field, thus we need to fetch mediated requests and only if they are CONFIRMED -
      // we can populate circulation request id in the response
      updateConfirmedRequestId(batchDetails);
    }

    return batchDetails;
  }

  private void updateConfirmedRequestId(BatchRequestDetailsResponse batchDetails) {
    var idsToRequestDetail = batchDetails.getMediatedBatchRequestDetails().stream()
      .filter(request -> Objects.nonNull(request.getConfirmedRequestId()))
      .map(request -> Map.entry(request.getConfirmedRequestId(), request))
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    var statusQuery = CqlQuery.exactMatchAny("mediatedRequestStatusText", List.of("Open", "Closed"))
      .and(new CqlQuery("requestLevelText=Item"));
    var mediatedRequestsById = Lists.partition(new ArrayList<>(idsToRequestDetail.keySet()), batchRequestDetailsQueryIdsSize).stream()
      .map(ids -> CqlQuery.exactMatchAnyId(ids).and(statusQuery))
      .map(cqlQuery -> requestMediatedClient.getMediatedRequestsByQuery(cqlQuery.query()).getMediatedRequests())
      .flatMap(List::stream)
      .filter(mediatedRequest -> isNotBlank(mediatedRequest.getConfirmedRequestId()))
      .collect(Collectors.toMap(MediatedRequest::getId, Function.identity()));

    var updatedDetails = new ArrayList<>(batchDetails.getMediatedBatchRequestDetails());
    CollectionUtils.transform(updatedDetails, detail ->
      updateConfirmedRequestIdForBatchRequestDetail(detail, mediatedRequestsById));
    batchDetails.setMediatedBatchRequestDetails(updatedDetails);
  }

  private BatchRequestDetail updateConfirmedRequestIdForBatchRequestDetail(BatchRequestDetail detail,
                                                                           Map<String, MediatedRequest> mediatedRequestsById) {
    var mediatedRequestId = detail.getConfirmedRequestId();
    if (mediatedRequestId == null) {
      return detail;
    }

    if (mediatedRequestsById.containsKey(mediatedRequestId)) {
      var mediatedRequest = mediatedRequestsById.get(mediatedRequestId);
      detail.setConfirmedRequestId(mediatedRequest.getConfirmedRequestId());
    } else {
      // if mediated request is not confirmed yet (means it's status is NEW or no circulation request is created for it yet)
      // then we show nothing in confirmedRequestId field for batch request detail
      detail.setConfirmedRequestId(null);
    }

    return detail;
  }
}
