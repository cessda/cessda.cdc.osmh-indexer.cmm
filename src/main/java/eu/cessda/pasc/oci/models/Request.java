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

import lombok.NonNull;
import org.jdom2.Document;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.List;

public record Request(@Nullable URI baseURL, @NonNull List<Record> records) {
    /**
     * Create a synthetic request the given documents root element set as the metadata holder.
     */
    public static Request createSyntheticRequest(Document document) {
        return new Request(
            null,
            List.of(new Record(null, document))
        );
    }
}
