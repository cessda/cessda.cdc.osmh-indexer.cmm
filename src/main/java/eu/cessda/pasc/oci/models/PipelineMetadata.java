/*
 * Copyright © 2017-2023 CESSDA ERIC (support@cessda.eu)
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Value;

import java.net.URI;

/**
 * Model for metadata passed in the CESSDA metadata pipeline.
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true) // Some parts of the JSON are not relevant to the indexer
public class PipelineMetadata {
    /**
     * The identifier of the repository.
     */
    String code;
    /**
     * The friendly name of the repository.
     */
    String name;
    /**
     * The default language to use when a metadata record does not specify a language.
     */
    String defaultLanguage;
    /**
     * the base URL of the repository.
     */
    URI url;
    /**
     * the DDI version harvested from the remote repository.
     */
    String ddiVersion;
}
