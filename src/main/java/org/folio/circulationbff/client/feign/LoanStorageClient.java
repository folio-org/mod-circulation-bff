package org.folio.circulationbff.client.feign;

import org.folio.circulationbff.domain.dto.Loans;
import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "loan-storage", url = "loan-storage/loans", configuration = FeignClientConfiguration.class)
public interface LoanStorageClient extends GetByQueryClient<Loans> {

}
