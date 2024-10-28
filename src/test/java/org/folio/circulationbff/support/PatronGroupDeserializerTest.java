package org.folio.circulationbff.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.folio.circulationbff.domain.dto.PatronGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

class PatronGroupDeserializerTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    SimpleModule module = new SimpleModule();
    module.addDeserializer(Object.class, new PatronGroupDeserializer());
    objectMapper.registerModule(module);
  }

  @Test
  void shouldDeserializeTextualJsonToString() throws Exception {
    String json = "\"patronGroup\": \"3684a786-6671-4268-8ed0-9db82ebca60b\"";
    Object result = objectMapper.readValue(json, Object.class);
    assertTrue(result instanceof String, "Result should be a String");
  }

  @Test
  void shouldDeserializeObjectJsonToPatronGroup() throws Exception {
    String json = "{ \"id\": \"3684a786-6671-4268-8ed0-9db82ebca60b\", \"group\": \"Patron\" }";
    Object result = objectMapper.readValue(json, Object.class);

    PatronGroup patronGroup = (PatronGroup) result;
    assertEquals("3684a786-6671-4268-8ed0-9db82ebca60b", patronGroup.getId());
    assertEquals("Patron", patronGroup.getGroup());
  }
}
