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
package eu.cessda.pasc.oci.models.configurations;

import lombok.Data;

import java.io.Serializable;
import java.net.URI;

/**
 * Repo configuration model
 *
 * @author moses AT doraventures DOT com
 */
@Data
public class Repo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The URL of the repository.
     */
    private URI url;
    /**
     * The short name of the repository.
     */
    private String code;
    /**
     * The friendly name of the repository.
     */
    private String name;
    /**
     * The repository handler.
     */
    private String handler;
    /**
     * The {@code metadataPrefix} of the metadata format to be retrieved from the repository.
     *
     * @see <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#MetadataNamespaces">http://www.openarchives.org/OAI/openarchivesprotocol.html#MetadataNamespaces</a>
     */
    private String preferredMetadataParam;
    /**
     * The set of metadata records to be retrieved from the remote repository.
     *
     * @see <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html#Set">http://www.openarchives.org/OAI/openarchivesprotocol.html#Set</a>
     */
    private String setSpec;
    /**
     * The default language, overrides the global default if set
     */
    private String defaultLanguage;
}
