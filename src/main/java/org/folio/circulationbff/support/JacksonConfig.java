package org.folio.circulationbff.support;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

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
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    objectMapper.setDateFormat(dateFormat);
    return objectMapper;
  }
}
