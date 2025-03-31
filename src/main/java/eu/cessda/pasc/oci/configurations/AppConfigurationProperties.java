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

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.*;

/**
 * Loads Default Configurations from application*.yml
 *
 * @author moses AT doraventures DOT com
 */
@Slf4j
@ConfigurationProperties
public record AppConfigurationProperties(
    Path baseDirectory,
    Set<String> languages,
    OaiPmh oaiPmh,
    List<Repo> repos
) {

    private static final Set<String> SUPPORTED_LANGUAGES = new LinkedHashSet<>(List.of("cs", "da", "de", "el", "en", "et", "fi", "fr", "hu", "is", "it", "nl", "no", "pt", "sk", "sl", "sr", "sv"));

    public AppConfigurationProperties(Path baseDirectory, Set<String> languages, OaiPmh oaiPmh, List<Repo> repos) {
        this.repos = Objects.requireNonNullElseGet(repos, Collections::emptyList);
        this.oaiPmh = Objects.requireNonNullElseGet(oaiPmh, OaiPmh::new);
        this.baseDirectory = baseDirectory;

        if (languages == null || languages.isEmpty()) {
            // Enable all supported languages by default
            this.languages = SUPPORTED_LANGUAGES;
        } else {
            this.languages = new HashSet<>(languages.size());

            // Flag if unsupported languages are specified
            boolean unsupportedLanguage = false;

            for (var language : languages) {
                // Check if the language is supported (i.e. has an Elasticsearch settings configuration)
                if (SUPPORTED_LANGUAGES.contains(language)) {
                    this.languages.add(language);
                } else {
                    unsupportedLanguage = true;
                    log.warn("Language \"{}\" is not supported", language);
                }
            }

            if (unsupportedLanguage) {
                log.warn("Supported languages: {}", SUPPORTED_LANGUAGES);
            }
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
