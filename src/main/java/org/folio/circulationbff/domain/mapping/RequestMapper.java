package org.folio.circulationbff.domain.mapping;

import org.folio.circulationbff.domain.dto.EcsRequestExternal;
import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface RequestMapper {

  @Mapping(target = "requestLevel", qualifiedByName = "externalToMediatedRequestLevel")
  @Mapping(target = "fulfillmentPreference", qualifiedByName = "externalToMediatedFulfillmentPreference")
  @Mapping(target = "requestType", constant = "PAGE")
  @Mapping(source = "itemId", target = "itemId")
  @Mapping(source = "holdingsRecordId", target = "holdingsRecordId")
  @Mapping(source = "instanceId", target = "instanceId")
  @Mapping(source = "requesterId", target = "requesterId")
  @Mapping(source = "pickupServicePointId", target = "pickupServicePointId")
  @Mapping(source = "requestDate", target = "requestDate")
  @Mapping(source = "patronComments", target = "patronComments")
  MediatedRequest toMediatedRequest(EcsRequestExternal externalRequest);

  @Named("externalToMediatedRequestLevel")
  default MediatedRequest.RequestLevelEnum externalToMediatedRequestLevel(
    EcsRequestExternal.RequestLevelEnum ecsRequestExternalRequestLevel) {

    return ecsRequestExternalRequestLevel != null
      ? MediatedRequest.RequestLevelEnum.fromValue(ecsRequestExternalRequestLevel.getValue())
      : null;
  }

  @Named("externalToMediatedFulfillmentPreference")
  default MediatedRequest.FulfillmentPreferenceEnum externalToMediatedFulfillmentPreference(
    EcsRequestExternal.FulfillmentPreferenceEnum ecsRequestExternalFulfillmentPreference) {

    return ecsRequestExternalFulfillmentPreference != null
      ? MediatedRequest.FulfillmentPreferenceEnum.fromValue(ecsRequestExternalFulfillmentPreference.getValue())
      : null;
  }

}
