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
package eu.cessda.pasc.oci.helpers.logging;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.boot.logging.LogLevel;

import java.time.LocalDateTime;

/**
 * Pojo to hold details of remote detail. To be used for logging JSON
 *
 * @author moses AT doraventures DOT com
 */
@JsonInclude()
@JsonPropertyOrder({
    "logLevel",
    "responseCode",
    "responseMessage",
    "occurredAt"
})
@Builder
@Getter
@ToString
public class RemoteResponse {
  LogLevel logLevel;
  int responseCode;
  String responseMessage;
  LocalDateTime occurredAt;
}
