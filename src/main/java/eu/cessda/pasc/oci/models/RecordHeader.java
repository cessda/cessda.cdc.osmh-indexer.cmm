/*
 * Copyright © 2017-2025 CESSDA ERIC (support@cessda.eu)
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

import lombok.Builder;

/**
 * Represents an OAI-PMH header.
 *
 * @param deleted Deletion status on the remote repository
 * @author moses AT doraventures DOT com
 * @see <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#Record">http://www.openarchives.org/OAI/openarchivesprotocol.html#Record</a>
 */
@Builder
public record RecordHeader(
    String lastModified,
    String identifier,
    boolean deleted
) {
}
