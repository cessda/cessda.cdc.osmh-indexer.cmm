package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

/**
 * Harvester (OSMH) Handler concept Constants
 *
 * @author moses@doraventures.com
 */
public class HandlerConstants {

  public static final String STUDY = "Study";
  public static final String UNSUCCESSFUL_RESPONSE = "Unsuccessful response from remote repository.";
  static final String NOT_AVAIL = "not available";

  private HandlerConstants() {
    throw new UnsupportedOperationException("Utility class, instantiation not allow");
  }

  public static final String UNSUPPORTED_API_VERSION = "Unsupported API-version [%s]";
  public static final String SYSTEM_ERROR = "System error!";
  public static final String SUCCESSFUL_OPERATION = "Successful operation!";
  public static final String BAD_REQUEST = "Bad request!";
  public static final String NOT_FOUND = "Not found!";
  public static final String THE_GIVEN_URL_IS_NOT_FOUND = "The given url is not found!";

  public static final String RETURN_404_FOR_OTHER_PATHS = "Return 404 for other paths.";
  public static final String MESSAGE = "message";
  public static final String RECORD_HEADER = "RecordHeader";
}
