/*
 * Copyright © 2017-2020 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Value;

/**
 * Represents an OAI-PMH header.
 *
 * @author moses AT doraventures DOT com
 * @see <a href=http://www.openarchives.org/OAI/openarchivesprotocol.html#Record>http://www.openarchives.org/OAI/openarchivesprotocol.html#Record</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "lastModified",
    "type",
    "recordType",
    "identifier",
    "deleted"
})
@Builder
@Value
public class RecordHeader {

    @JsonProperty("lastModified")
    String lastModified;
    @JsonProperty("type")
    String type;
    @JsonProperty("recordType")
    String recordType;
    @JsonProperty("identifier")
    String identifier;
    /**
     * Deletion status on the remote repository
     */
    @JsonProperty("deleted")
    boolean deleted;
}
