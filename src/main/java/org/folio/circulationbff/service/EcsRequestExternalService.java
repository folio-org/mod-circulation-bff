package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.EcsRequestExternal;
import org.folio.circulationbff.domain.dto.EcsTlr;

public interface EcsRequestExternalService {
  EcsTlr createEcsRequestExternal(EcsRequestExternal ecsRequestExternal);
}
