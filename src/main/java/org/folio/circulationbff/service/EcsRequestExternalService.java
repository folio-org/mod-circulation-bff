package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.EcsRequestExternal;
import org.folio.circulationbff.domain.dto.Request;

public interface EcsRequestExternalService {
  Request createEcsRequestExternal(EcsRequestExternal ecsRequestExternal);
}
