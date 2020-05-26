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

package eu.cessda.pasc.oci.exception;

import lombok.Getter;
import lombok.NonNull;

/**
 * Exception for internally encountered Exceptions
 *
 * @author moses AT doraventures DOT com
 */
public class ExternalSystemException extends CustomHandlerException {

  private static final long serialVersionUID = 928798312826959273L;

  @Getter
  private final String externalResponseBody;

  /**
   * Constructs an ExternalSystemException with the specified message and cause.
   *
   * @param message the detail message
   * @param cause   the cause
   */
  public ExternalSystemException(String message, Throwable cause) {
    super(message, cause);
    externalResponseBody = null;
  }

  /**
   * Constructs an ExternalSystemException with the specified message, cause and external response body.
   *
   * @param message              the detail message
   * @param cause                the cause
   * @param externalResponseBody the external response that caused this exception
   */
  public ExternalSystemException(String message, Throwable cause, @NonNull String externalResponseBody) {
    super(message, cause);
    this.externalResponseBody = externalResponseBody;
  }
}
