package eu.cessda.pasc.osmhhandler.oaipmh.models.cmmstudy;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;

/**
 * CMMStudy Serialize/deserialize helpers.
 * <p>
 * Then you can deserialize a JSON string with
 * <pre>
 * {@code
 *  CMMStudy data = Converter.fromJsonString("{\"json\":\"String\"}");
 *  }
 * </pre>
 *
 * @author moses@doraventures.com
 */
public class CMMConverter {

  // Serialize/deserialize helpers

  public static CMMStudy fromJsonString(String json) throws IOException {
    return getObjectReader().readValue(json);
  }

  public static String toJsonString(CMMStudy obj) throws JsonProcessingException {
    return getObjectWriter().writeValueAsString(obj);
  }

  private static ObjectReader reader;
  private static ObjectWriter writer;

  private static void instantiateMapper() {
    ObjectMapper mapper = new ObjectMapper();
    reader = mapper.readerFor(CMMStudy.class);
    writer = mapper.writerFor(CMMStudy.class);
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
