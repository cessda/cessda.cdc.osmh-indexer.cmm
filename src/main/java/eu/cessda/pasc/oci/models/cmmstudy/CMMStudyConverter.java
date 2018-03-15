package eu.cessda.pasc.oci.models.cmmstudy;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

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
public class CMMStudyConverter {

  // Serialize/deserialize helpers

  public static CMMStudy fromJsonString(String json) throws IOException {
    return getObjectReader().readValue(json);
  }

  private static ObjectReader reader;

  private static void instantiateMapper() {
    ObjectMapper mapper = new ObjectMapper();
    reader = mapper.readerFor(CMMStudy.class);
  }

  private static ObjectReader getObjectReader() {
    if (reader == null) instantiateMapper();
    return reader;
  }
}
