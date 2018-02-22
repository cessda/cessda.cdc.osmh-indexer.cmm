package eu.cessda.pasc.oci.models.cmmstudy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.IOException;

/**
 * @author moses@doraventures.com
 */
public enum  RecordType {
  CMM_STUDY;

  @JsonValue
  public String toValue() {
    switch (this) {
      case CMM_STUDY: return "CMMStudy";
    }
    return null;
  }

  @JsonCreator
  public static RecordType forValue(String value) throws IOException {
    if (value.equals("CMMStudy")) return CMM_STUDY;
    throw new IOException("Cannot deserialize RecordType");
  }
}
