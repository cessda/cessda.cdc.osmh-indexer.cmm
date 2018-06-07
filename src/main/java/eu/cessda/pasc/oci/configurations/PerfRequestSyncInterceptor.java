package eu.cessda.pasc.oci.configurations;

import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * HTTP request interceptor for logging response times
 *
 * @author moses@doraventures.com
 */
@Component
@Slf4j
public class PerfRequestSyncInterceptor implements ClientHttpRequestInterceptor {

  @Override
  public ClientHttpResponse intercept(HttpRequest hr, byte[] bytes, ClientHttpRequestExecution chre) throws IOException {
    Stopwatch stopwatch = Stopwatch.createStarted();
    ClientHttpResponse response = chre.execute(hr, bytes);
    stopwatch.stop();

    if (log.isDebugEnabled()) {
      final String msg = "X[{}], uri=[{}], Took=[{}]ms, Code=[{}]";
      log.debug(
          msg, hr.getMethod(), hr.getURI(), stopwatch.elapsed(TimeUnit.MILLISECONDS), response.getStatusCode().value());
    }
    return response;
  }
}
