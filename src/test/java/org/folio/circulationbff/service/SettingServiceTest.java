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
  private TenantService tenantService;

  @InjectMocks
  private SettingsServiceImpl service;

  @ParameterizedTest
  @MethodSource("settingsToResponse")
  void isEcsTlrSettingsEnabledTest(boolean isCentralTenant, Boolean isTlrEnabled,
    Boolean isCirculationTlrEnabled, boolean expectedValue) {

    when(tenantService.isCurrentTenantCentral()).thenReturn(isCentralTenant);
    mockByIsCentralTenantId(isCirculationTlrEnabled, isTlrEnabled, isCentralTenant);

    assertThat(service.isEcsTlrFeatureEnabled(), equalTo(expectedValue));
  }

  private void mockByIsCentralTenantId(Boolean isCirculationTlrEnabled, Boolean isTlrEnabled,
    boolean isCentralTenant) {

    if (BooleanUtils.isTrue(isCentralTenant)) {
      when(ecsTlrClient.getTlrSettings()).thenReturn(new TlrSettings()
        .ecsTlrFeatureEnabled(isTlrEnabled));
    } else {
      when(circulationClient.getCirculationSettingsByQuery(anyString()))
        .thenReturn(new CirculationSettingsResponse()
          .totalRecords(1)
          .addCirculationSettingsItem(new CirculationSettings()
            .value(new CirculationSettingsValue()
              .enabled(isCirculationTlrEnabled))));
    }
  }

  private static Stream<Arguments> settingsToResponse() {
    return Stream.of(
      Arguments.of(true, true, null, true),
      Arguments.of(true, false, null, false),
      Arguments.of(false, null, true, true),
      Arguments.of(false, null, false, false)
    );
  }
}

