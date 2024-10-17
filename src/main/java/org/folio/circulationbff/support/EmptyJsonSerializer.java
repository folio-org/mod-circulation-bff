package org.folio.circulationbff.support;

import java.io.IOException;

import org.folio.circulationbff.domain.dto.EmptyBffSearchInstance;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class EmptyJsonSerializer extends StdSerializer<EmptyBffSearchInstance> {

  public EmptyJsonSerializer() {
    super(EmptyBffSearchInstance.class);
  }

  @Override
  public void serialize(EmptyBffSearchInstance value, JsonGenerator gen, SerializerProvider provider)
    throws IOException {

    gen.writeStartObject();
    gen.writeEndObject();
  }
}