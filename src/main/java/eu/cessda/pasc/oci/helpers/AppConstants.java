package eu.cessda.pasc.oci.helpers;

import java.util.TimeZone;

/**
 * Harvester (OSMH) Handler concept Constants
 *
 * @author moses@doraventures.com
 */
public class AppConstants {

  public static final String UNSUCCESSFUL_RESPONSE = "Unsuccessful response from remote repository.";
  static final String[] EXPECTED_DATE_FORMATS = new String[]{"yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd"};
  static final String UTC_ID = TimeZone.getTimeZone("UTC").getID();

  private AppConstants() {
    throw new UnsupportedOperationException("Utility class, instantiation not allow");
  }
}
