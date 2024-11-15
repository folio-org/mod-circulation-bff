package org.folio.circulationbff.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MessageBuilder {
  public static String buildLogMessageForStaffSlipsFetching(boolean isEcsTlrFeatureEnabled) {
    String ecsTlrFeatureValueMessage = isEcsTlrFeatureEnabled ? "enabled" : "disabled";
    String staffSlipsSourceModuleName = isEcsTlrFeatureEnabled ? "mod-tlr" : "mod-circulation";

    return String.format("Ecs TLR Feature is %s. Getting " +
      "staff slips from %s", ecsTlrFeatureValueMessage, staffSlipsSourceModuleName);
  }
}
