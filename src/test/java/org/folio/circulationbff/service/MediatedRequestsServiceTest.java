package org.folio.circulationbff.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.UUID;

import org.folio.circulationbff.client.feign.RequestMediatedClient;
import org.folio.circulationbff.domain.dto.MediatedRequest;
import org.folio.circulationbff.service.impl.MediatedRequestsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class MediatedRequestsServiceTest {

  @Mock
  private RequestMediatedClient requestMediatedClient;
  @InjectMocks
  private MediatedRequestsServiceImpl mediatedRequestsService;
  private static final String REQUEST_ID = UUID.randomUUID().toString();

  @Test
  void updateAndConfirmMediatedRequestShouldConfirmRequest() {
    when(requestMediatedClient.putRequestMediated(anyString(), any(MediatedRequest.class)))
      .thenReturn(new ResponseEntity<>(NO_CONTENT));
    when(requestMediatedClient.confirmRequestMediated(anyString(), any(MediatedRequest.class)))
      .thenReturn(new ResponseEntity<>(OK));
    MediatedRequest mediatedRequest = new MediatedRequest().id(REQUEST_ID);
    ResponseEntity<Void> response = mediatedRequestsService.updateAndConfirmMediatedRequest(
      mediatedRequest);

    assertEquals(OK, response.getStatusCode());
    verify(requestMediatedClient).confirmRequestMediated(REQUEST_ID, mediatedRequest);
  }

  @Test
  void updateAndConfirmMediatedRequestShouldFailsIfBadRequest() {
    when(requestMediatedClient.putRequestMediated(anyString(), any(MediatedRequest.class)))
      .thenReturn(new ResponseEntity<>(BAD_REQUEST));
    MediatedRequest mediatedRequest = new MediatedRequest().id(REQUEST_ID);
    ResponseEntity<Void> response = mediatedRequestsService.updateAndConfirmMediatedRequest(
      mediatedRequest);

    assertEquals(BAD_REQUEST, response.getStatusCode());
    verify(requestMediatedClient, never()).confirmRequestMediated(anyString(), any(
      MediatedRequest.class));
  }
}
