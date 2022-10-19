/*
 * Copyright © 2017-2021 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.models.cmmstudy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Data
public class RelatedPublication {
    /**
     * The title of the related publication.
     */
    @JsonProperty("title")
    String title;
    /**
     * The URI of the holdings of the related publication.
     */
    @JsonProperty("holdings")
    List<URI> holdings;

    public List<URI> getHoldings() {
        if (holdings == null) {
            holdings = new ArrayList<>();
        }
        return holdings;
    }
}
