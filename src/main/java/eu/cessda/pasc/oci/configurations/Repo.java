/*
 * Copyright © 2017-2024 CESSDA ERIC (support@cessda.eu)
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

import java.net.URI;
import java.nio.file.Path;

/**
 * Repo configuration model
 *
 * @param url                    The URL of the repository.
 * @param path                   The path to the repository directory.
 * @param code                   The short name of the repository.
 * @param name                   The friendly name of the repository.
 * @param preferredMetadataParam The {@code metadataPrefix} of the metadata format to be retrieved from the repository.
 * @param setSpec                The set of metadata records to be retrieved from the remote repository.
 * @param defaultLanguage        The default language, overrides the global default if set
 * @author moses AT doraventures DOT com
 */
public record Repo(
    URI url,
    Path path,
    String code,
    String name,
    String preferredMetadataParam,
    String setSpec,
    String defaultLanguage
) {
}
