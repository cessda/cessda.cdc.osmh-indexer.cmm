package eu.cessda.pasc.oci.helpers;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.boot.logging.LogLevel;
import org.springframework.http.HttpStatus;

import static org.mockito.Mockito.*;

/**
 * @author moses@doraventures.com
 */
@Slf4j
public class LogHelperTest {

  @Test
  public void shouldCallLoggerWithExpectedLogLevel() {

    Logger logger = mock(Logger.class);
    LogHelper.logResponse(HttpStatus.BAD_REQUEST, logger, LogLevel.ERROR);
    verify(logger, times(1)).error(anyString());

    logger = mock(Logger.class);
    LogHelper.logResponse(HttpStatus.BAD_REQUEST, logger, LogLevel.INFO);
    verify(logger, times(1)).info(anyString());

    logger = mock(Logger.class);
    LogHelper.logResponse(HttpStatus.BAD_REQUEST, logger, LogLevel.DEBUG);
    verify(logger, times(1)).debug(anyString());
  }
}