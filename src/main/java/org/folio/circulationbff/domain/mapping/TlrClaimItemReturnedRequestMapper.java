package org.folio.circulationbff.domain.mapping;

import java.util.UUID;

import org.folio.circulationbff.domain.dto.ClaimItemReturnedRequest;
import org.folio.circulationbff.domain.dto.TlrClaimItemReturnedRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TlrClaimItemReturnedRequestMapper {
  TlrClaimItemReturnedRequest toTlrClaimItemReturnedRequest(UUID loanId, ClaimItemReturnedRequest claimItemReturnedRequest);
}

