package eu.cessda.pasc.oci.helpers;

import java.util.TimeZone;

/**
 * Harvester (OSMH) Handler concept Constants
 *
 * @author moses@doraventures.com
 */
public class AppConstants {

  public static final String UNSUCCESSFUL_RESPONSE = "Unsuccessful response from remote repository.";
  static final String[] EXPECTED_DATE_FORMATS = new String[]{"yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd", "yyyy-MM-dd'T'HH:mm:ssZ"};
  static final String UTC_ID = TimeZone.getTimeZone("UTC").getID();
  public static final String LAST_MODIFIED_FIELD = "lastModified";

  private AppConstants() {
    throw new UnsupportedOperationException("Utility class, instantiation not allow");
  }
}
