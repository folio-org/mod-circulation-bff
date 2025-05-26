package org.folio.circulationbff.service.impl;

import static java.lang.Boolean.TRUE;
import static java.util.stream.Collectors.toMap;
import static org.folio.circulationbff.domain.mapping.CirculationLoanMapper.toStream;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.domain.dto.BffSearchInstance;
import org.folio.circulationbff.domain.dto.BffSearchItem;
import org.folio.circulationbff.domain.dto.CirculationLoan;
import org.folio.circulationbff.domain.dto.CirculationLoans;
import org.folio.circulationbff.domain.dto.LoanItem;
import org.folio.circulationbff.domain.mapping.CirculationLoanMapper;
import org.folio.circulationbff.service.CirculationLoanService;
import org.folio.circulationbff.service.SearchService;
import org.folio.circulationbff.service.SettingsService;
import org.folio.circulationbff.service.TenantService;
import org.folio.circulationbff.support.CqlQuery;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class CirculationLoanServiceImpl implements CirculationLoanService {

  private final SearchService searchService;
  private final TenantService tenantService;
  private final SettingsService settingsService;
  private final CirculationClient circulationClient;
  private final CirculationLoanMapper circulationLoanMapper;

  @Override
  public CirculationLoans findCirculationLoans(String query, Integer limit,
    Integer offset, String totalRecords) {
    var circulationLoans = circulationClient.findLoansByQuery(query, limit, offset, totalRecords);

    var dcbItemIds = circulationLoans.getLoans().stream()
      .filter(CirculationLoanServiceImpl::shouldEnrichCirculationLoan)
      .map(CirculationLoan::getItemId)
      .toList();

    if (CollectionUtils.isEmpty(dcbItemIds) || !isEnrichOperationAllowed()) {
      return circulationLoans;
    }

    var bffInstances = getBffInstancesByItemId(dcbItemIds);

    for (var loan : circulationLoans.getLoans()) {
      if (shouldEnrichCirculationLoan(loan)) {
        var loanItem = loan.getItem();
        var bffSearchInstance = bffInstances.get(loan.getItemId());
        if (bffSearchInstance == null) {
          log.warn("findCirculationLoans:: instance not found by itemId: {}", loan.getItemId());
          continue;
        }

        var enrichedItem = getEnrichedItem(loanItem, bffSearchInstance);
        loan.setItem(enrichedItem);
      }
    }

    return circulationLoans;
  }

  @Override
  public CirculationLoan getCirculationLoanById(UUID loanId) {
    var circulationLoan = circulationClient.findLoanById(loanId);
    if (!(shouldEnrichCirculationLoan(circulationLoan) && isEnrichOperationAllowed())) {
      return circulationLoan;
    }

    var itemId = circulationLoan.getItemId();
    var bffSearchInstancesMap = getBffInstancesByItemId(List.of(itemId));
    var bffSearchInstance = bffSearchInstancesMap.get(itemId);
    if (bffSearchInstance == null) {
      log.warn("getCirculationLoanById:: instance not found by itemId: {}", itemId);
      return circulationLoan;
    }

    circulationLoan.setItem(getEnrichedItem(circulationLoan.getItem(), bffSearchInstance));
    return circulationLoan;
  }

  private boolean isEnrichOperationAllowed() {
    return settingsService.isEcsTlrFeatureEnabled() && tenantService.isCurrentTenantCentral();
  }

  private static boolean shouldEnrichCirculationLoan(CirculationLoan loan) {
    return TRUE.equals(loan.getIsDcb()) && loan.getItem() != null;
  }

  private LoanItem getEnrichedItem(LoanItem loanItem, BffSearchInstance bffSearchInstance) {
    var loanItemId = loanItem.getId();
    var bffSearchItem = getBffSearchItem(loanItemId, bffSearchInstance);
    return circulationLoanMapper.enrichLoan(loanItem, bffSearchInstance, bffSearchItem);
  }

  private Map<String, BffSearchInstance> getBffInstancesByItemId(List<String> itemIds) {
    var cqlQuery = CqlQuery.exactMatchAny("item.id", itemIds);
    return searchService.findInstances(cqlQuery.toString()).stream()
      .flatMap(CirculationLoanServiceImpl::getInstanceByItemEntry)
      .collect(toMap(Entry::getKey, Entry::getValue, (o1, o2) -> o2));
  }

  private static Stream<Entry<String, BffSearchInstance>> getInstanceByItemEntry(BffSearchInstance instance) {
    return ListUtils.emptyIfNull(instance.getItems()).stream()
      .map(item -> new SimpleImmutableEntry<>(item.getId(), instance));
  }

  private static BffSearchItem getBffSearchItem(String itemId, BffSearchInstance searchInstance) {
    return toStream(searchInstance.getItems())
      .filter(bffSearchItem -> Objects.equals(bffSearchItem.getId(), itemId))
      .findFirst()
      .orElse(null);
  }
}
