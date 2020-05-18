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
package eu.cessda.pasc.oci.helpers.exception;

import lombok.NonNull;
import lombok.Value;

import java.io.IOException;
import java.io.Serializable;
import java.util.Optional;

/**
 * Exception for external encountered Exceptions
 *
 * @author moses AT doraventures DOT com
 */
public class ExternalSystemException extends Exception {

  private static final long serialVersionUID = 928798312826959273L;

  private final ExternalResponse externalResponse;

  /**
   * Constructs an ExternalSystemException with the specified message and cause.
   *
   * @param message the detail message
   * @param cause   the cause
   */
  public ExternalSystemException(@NonNull String message, @NonNull IOException cause) {
    super(message, cause);
    externalResponse = null;
  }

  /**
   * Constructs an ExternalSystemException with the specified message, cause and external response body.
   *
   * @param message              the detail message
   * @param statusCode           the status code of the external response that caused this exception
   * @param externalResponseBody the body of the external response that caused this exception
   */
  public ExternalSystemException(@NonNull String message, int statusCode, @NonNull String externalResponseBody) {
    super(message);
    externalResponse = new ExternalResponse(externalResponseBody, statusCode);
  }

  /**
   * Gets the external response that caused this exception, or an empty optional if no response was received.
   */
  public Optional<ExternalResponse> getExternalResponse() {
    return Optional.ofNullable(externalResponse);
  }

  /**
   * An immutable object describing the status code and the body of the external response.
   */
  @Value
  public static class ExternalResponse implements Serializable {
    private static final long serialVersionUID = -7110617275735794989L;
    @NonNull
    String externalResponseBody;
    int statusCode;
  }
}
