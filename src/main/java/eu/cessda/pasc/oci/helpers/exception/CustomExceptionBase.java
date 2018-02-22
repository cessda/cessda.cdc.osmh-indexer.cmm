package eu.cessda.pasc.oci.helpers.exception;

/**
 * Custom Exception
 *
 * @author moses@doraventures.com
 */
public class CustomExceptionBase extends Exception{

  private static final long serialVersionUID = 5715687019114712665L;

  CustomExceptionBase(String message) {
    super(message);
  }

  CustomExceptionBase(String message, Throwable cause) {
    super(message, cause);
  }
}
