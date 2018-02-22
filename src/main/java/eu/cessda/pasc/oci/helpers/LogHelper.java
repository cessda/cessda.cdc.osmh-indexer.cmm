package eu.cessda.pasc.oci.helpers;

import eu.cessda.pasc.oci.helpers.logging.RemoteResponse;
import org.slf4j.Logger;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Log Helper
 *
 * @author moses@doraventures.com
 */
public class LogHelper {

  private static final String ERROR = "ERROR";

  private LogHelper() {
    throw new UnsupportedOperationException("Utility class, instantiation not allow");
  }

  public static void logResponse(HttpStatus statusCode, Logger log, LogLevel logLevel) {

    String msg = RemoteResponse.builder()
        .logLevel(logLevel)
        .responseCode(statusCode.value())
        .responseMessage(statusCode.getReasonPhrase())
        .occurredAt(LocalDateTime.now()).build().toString();

    String s = logLevel.toString();
    if (ERROR.equals(s)) {
      log.error(msg);
    }
  }
}
