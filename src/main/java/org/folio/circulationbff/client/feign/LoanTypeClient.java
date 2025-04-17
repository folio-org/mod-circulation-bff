package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.LoanType;
import org.folio.circulationbff.domain.dto.LoanTypes;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "loan-types", url = "loan-types",
  configuration = FeignClientConfiguration.class)
public interface LoanTypeClient extends GetByQueryClient<LoanTypes> {

  @GetMapping("/{id}")
  LoanType findLoanType(@PathVariable String id);
}
