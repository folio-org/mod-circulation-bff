package org.folio.circulationbff.service;

import org.folio.circulationbff.domain.dto.EcsRequestExternal;
import org.folio.circulationbff.domain.dto.PostEcsRequestExternal201Response;
import org.folio.circulationbff.domain.dto.Request;

public interface EcsRequestExternalService {
  Object createEcsRequestExternal(EcsRequestExternal ecsRequestExternal);
}
