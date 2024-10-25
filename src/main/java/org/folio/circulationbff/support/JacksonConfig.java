package org.folio.circulationbff.support;

import org.folio.circulationbff.domain.dto.BffRequest;
import org.folio.circulationbff.domain.dto.RequestRequester;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class JacksonConfig {

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.addMixIn(RequestRequester.class, RequestRequesterMixin.class);
    objectMapper.addMixIn(BffRequest.class, BffRequestMixin.class);
    return objectMapper;
  }
}
