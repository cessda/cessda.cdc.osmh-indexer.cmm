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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;


/**
 * Test for the PerfRequestSyncInterceptor
 *
 * @author moses AT doraventures DOT com
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(LoggerFactory.class)
@PowerMockIgnore("jdk.internal.reflect.*")
public class PerfRequestSyncInterceptorTest {

  @Mock
  private Logger loggerMock;

  @Before
  public void performBeforeEachTest() {
    PowerMockito.mockStatic(LoggerFactory.class);
    when(LoggerFactory.getLogger(any(Class.class))).thenReturn(loggerMock);
    when(loggerMock.isDebugEnabled()).thenReturn(true);
  }

  @Test
  public void intercept() throws IOException {

    // Given
    PerfRequestSyncInterceptor underTest = new PerfRequestSyncInterceptor();
    when(loggerMock.isDebugEnabled()).thenReturn(true);
    ClientHttpRequestExecution clientHttpRequestExecutionMock = mock(ClientHttpRequestExecution.class);
    ClientHttpResponse responseMock = mock(ClientHttpResponse.class);
    when(clientHttpRequestExecutionMock.execute(any(HttpRequest.class), any(byte[].class))).thenReturn(responseMock);
    when(responseMock.getStatusCode()).thenReturn(HttpStatus.I_AM_A_TEAPOT);

    // When
    underTest.intercept(mock(HttpRequest.class), new byte[Byte.parseByte("3")], clientHttpRequestExecutionMock);

    // Then
    verify(loggerMock, times(1)).debug(
        eq("X[{}] request for uri [{}] took [{}]ms.  Response code [{}]"),
        eq(null),
        eq(null),
        anyInt(),
        eq(418));
  }
}