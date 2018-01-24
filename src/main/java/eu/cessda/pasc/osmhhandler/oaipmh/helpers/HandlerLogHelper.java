package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.models.logging.RemoteResponse;
import org.slf4j.Logger;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Handler Log Helper
 *
 * @author moses@doraventures.com
 */
public class HandlerLogHelper {

  private HandlerLogHelper() {
    throw new UnsupportedOperationException("Hides implicit public constructor | For static constants only");
  }

  public static void logResponse(HttpStatus statusCode, Logger log, LogLevel logLevel) {

    String msg = RemoteResponse.builder()
        .logLevel(logLevel)
        .responseCode(statusCode.value())
        .responseMessage(statusCode.getReasonPhrase())
        .occurredAt(LocalDateTime.now()).build().toString();

    switch (logLevel.toString()) {
      case "ERROR":
        log.error(msg);
        break;
      case "WARN":
        log.warn(msg);
        break;
    }
  }
}
