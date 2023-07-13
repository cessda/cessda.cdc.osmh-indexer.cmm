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
package eu.cessda.pasc.oci.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Loads Default Configurations from application*.yml
 *
 * @author moses AT doraventures DOT com
 */
@ConfigurationProperties
public record AppConfigurationProperties(
    Path baseDirectory,
    List<String> languages,
    OaiPmh oaiPmh,
    List<Repo> repos
) {

    private static final List<String> DEFAULT_LANGUAGES = List.of("cs", "da", "de", "el", "en", "et", "fi", "fr", "hu", "it", "nl", "no", "pt", "sk", "sl", "sr", "sv");

    public AppConfigurationProperties(Path baseDirectory, List<String> languages, OaiPmh oaiPmh, List<Repo> repos) {
        this.repos = Objects.requireNonNullElseGet(repos, Collections::emptyList);
        this.oaiPmh = Objects.requireNonNullElseGet(oaiPmh, OaiPmh::new);
        this.baseDirectory = baseDirectory;

        if (languages == null || languages.isEmpty()) {
            this.languages = DEFAULT_LANGUAGES;
        } else {
            this.languages = languages;
        }
    }

    /**
     * OaiPmh configuration model
     *
     * @author moses AT doraventures DOT com
     */
    public record OaiPmh(
        MetadataParsingDefaultLang metadataParsingDefaultLang,
        String concatSeparator
    ) {
        private OaiPmh() {
            this(new MetadataParsingDefaultLang(), null);
        }
    }

    /**
     * Defaults for parsing metadata fields with no xml:lang specified,
     * where lang is extracted content is to be mapped against a lang
     *
     * @param active whether to fall back to a default language if xml:lang is not specified.
     * @param lang   the language to default to.
     * @author moses AT doraventures DOT com
     */
    public record MetadataParsingDefaultLang(boolean active, String lang) {
        private MetadataParsingDefaultLang() {
            this(false, null);
        }
    }
}
