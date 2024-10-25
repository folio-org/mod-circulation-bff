package org.folio.circulationbff.support;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

import org.folio.circulationbff.domain.dto.PatronGroup;

public class PatronGroupDeserializer extends JsonDeserializer<Object> {

  @Override
  public Object deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    JsonNode node = parser.getCodec().readTree(parser);
    if (node.isTextual()) {
      return node.asText();
    } else {
      return parser.getCodec().treeToValue(node, PatronGroup.class);
    }
  }
}
