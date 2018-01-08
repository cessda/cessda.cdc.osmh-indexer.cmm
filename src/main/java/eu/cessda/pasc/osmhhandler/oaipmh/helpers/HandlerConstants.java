package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

/**
 * General constants
 *
 * @author moses@doraventures.com
 */
public class HandlerConstants {

  private HandlerConstants() {
    // Private to keep class for static constants only
  }

  public static final String UNSUPPORTED_API_VERSION = "Unsupported API-version";
  public static final String SYSTEM_ERROR = "System error!";
  public static final String SUCCESSFUL_OPERATION = "Successful operation!";
  public static final String BAD_REQUEST = "Bad request!";
  public static final String NOT_FOUND = "Not found!";
  public static final String THE_GIVEN_URL_IS_NOT_FOUND = "The given url is not found!";

  public static final String RETURN_404_FOR_OTHER_PATHS = "Return 404 for other paths.";
  static final String MESSAGE = "message";
}
