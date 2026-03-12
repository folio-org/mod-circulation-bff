package org.folio.circulationbff.support;

import org.folio.circulationbff.domain.dto.BffSearchInstance;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class EmptyJsonSerializer extends StdSerializer<BffSearchInstance> {

  public EmptyJsonSerializer() {
    super(BffSearchInstance.class);
  }

  @Override
  public void serialize(BffSearchInstance value, JsonGenerator gen, SerializationContext provider)
    throws JacksonException {

    gen.writeStartObject();
    gen.writeEndObject();
  }
}
