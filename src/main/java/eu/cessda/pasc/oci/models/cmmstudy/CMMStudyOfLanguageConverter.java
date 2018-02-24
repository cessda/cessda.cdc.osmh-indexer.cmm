package eu.cessda.pasc.oci.models.cmmstudy;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.util.Optional;

/**
 * CMMStudyOfLanguage Serialize/deserialize helpers.
 * <p>
 * Then you can deserialize a JSON string with
 * <pre>
 * {@code
 *  CMMStudyOfLanguage data = Converter.fromJsonString("{\"json\":\"String\"}");
 *  }
 * </pre>
 *
 * @author moses@doraventures.com
 */
public class CMMStudyOfLanguageConverter {

  // Serialize/deserialize helpers

  public static CMMStudyOfLanguage fromJsonString(String json) throws IOException {
    return getObjectReader().readValue(json);
  }

  public static Optional<String> toJsonString(CMMStudyOfLanguage obj) {
    try {
      return Optional.ofNullable(getObjectWriter().writeValueAsString(obj));
    } catch (JsonProcessingException e) {
      return Optional.empty();
    }
  }

  private static ObjectReader reader;
  private static ObjectWriter writer;

  private static void instantiateMapper() {
    ObjectMapper mapper = new ObjectMapper();
    reader = mapper.readerFor(CMMStudyOfLanguage.class);
    writer = mapper.writerFor(CMMStudyOfLanguage.class);
  }

  private static ObjectReader getObjectReader() {
    if (reader == null) instantiateMapper();
    return reader;
  }

  private static ObjectWriter getObjectWriter() {
    if (writer == null) instantiateMapper();
    return writer;
  }
}
