package org.folio.circulationbff.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import org.folio.circulationbff.client.feign.CirculationClient;
import org.folio.circulationbff.client.feign.EcsTlrClient;
import org.folio.circulationbff.domain.dto.CirculationSettings;
import org.folio.circulationbff.domain.dto.CirculationSettingsResponse;
import org.folio.circulationbff.domain.dto.CirculationSettingsValue;
import org.folio.circulationbff.domain.dto.TlrSettings;
import org.folio.circulationbff.service.impl.SettingsServiceImpl;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.shaded.org.apache.commons.lang3.BooleanUtils;

@ExtendWith(MockitoExtension.class)
class SettingServiceTest {

  @Mock
  private EcsTlrClient ecsTlrClient;

  @Mock
  private CirculationClient circulationClient;

  @Mock
  private UserTenantsService userTenantsService;

  @InjectMocks
  private SettingsServiceImpl service;

  @ParameterizedTest
  @MethodSource("settingsToResponse")
  void isEcsTlrSettingsEnabledTest(boolean isCentralTenant, TlrSettings tlrSettings,
    CirculationSettingsResponse circulationSettingsResponse, boolean expectedValue) {

    when(userTenantsService.isCentralTenant()).thenReturn(isCentralTenant);
    mockByIsCentralTenantId(circulationSettingsResponse, tlrSettings, isCentralTenant);

    assertThat(service.isEcsTlrFeatureEnabled(), equalTo(expectedValue));
  }

  private void mockByIsCentralTenantId(CirculationSettingsResponse circulationSettingsResponse,
    TlrSettings tlrSettings, boolean isCentralTenant) {

    if (BooleanUtils.isTrue(isCentralTenant)) {
      when(ecsTlrClient.getTlrSettings()).thenReturn(tlrSettings);
    } else {
      when(circulationClient.getCirculationSettingsByQuery(anyString()))
        .thenReturn(circulationSettingsResponse);
    }
  }

  private static CirculationSettingsResponse buildCirculationSettingsResponse(
    boolean isEcsTlrEnabled) {

    CirculationSettingsResponse circulationSettingsResponse = new CirculationSettingsResponse();
    CirculationSettings circulationSettings = new CirculationSettings();
    CirculationSettingsValue value = new CirculationSettingsValue();
    value.enabled(isEcsTlrEnabled);
    circulationSettings.setValue(value);
    circulationSettingsResponse.addCirculationSettingsItem(circulationSettings);
    circulationSettingsResponse.setTotalRecords(1);
    return circulationSettingsResponse;
  }

  private static TlrSettings buildTlrSettings(boolean isTlrEnabled) {
    TlrSettings tlrSettings = new TlrSettings();
    tlrSettings.setEcsTlrFeatureEnabled(isTlrEnabled);
    return tlrSettings;
  }

  private static Stream<Arguments> settingsToResponse() {
    return Stream.of(
      Arguments.of(true, buildTlrSettings(true), null, true),
      Arguments.of(true, buildTlrSettings(false), null, false),
      Arguments.of(false, null, buildCirculationSettingsResponse(true),
        true),
      Arguments.of(false, null, buildCirculationSettingsResponse(false),
        false)
    );
  }

}
