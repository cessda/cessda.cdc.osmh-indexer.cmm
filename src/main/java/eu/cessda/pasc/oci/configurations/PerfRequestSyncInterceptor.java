/*
 * Copyright © 2017-2019 CESSDA ERIC (support@cessda.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cessda.pasc.oci.configurations;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * HTTP request interceptor for logging response times
 *
 * @author moses AT doraventures DOT com
 */
@Component
@Slf4j
public class PerfRequestSyncInterceptor implements ClientHttpRequestInterceptor {

  @Override
  public ClientHttpResponse intercept(HttpRequest hr, byte[] bytes, ClientHttpRequestExecution chre) throws IOException {
    Instant start = Instant.now();
    ClientHttpResponse response = chre.execute(hr, bytes);
    Instant stop = Instant.now();

    if (log.isDebugEnabled()) {
      log.debug("X[{}] request for uri [{}] took [{}]ms.  Response code [{}]",
              hr.getMethod(),
              hr.getURI(),
              Duration.between(start, stop).toMillis(),
              response.getStatusCode().value());
    }
    return response;
  }
}
