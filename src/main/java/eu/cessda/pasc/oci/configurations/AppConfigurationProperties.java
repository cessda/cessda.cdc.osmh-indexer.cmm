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
package eu.cessda.pasc.oci.configurations;

import eu.cessda.pasc.oci.models.configurations.Harvester;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.models.configurations.RestTemplateProps;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Loads Default Configurations from application*.yml
 *
 * @author moses AT doraventures DOT com
 */
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties
@Data
public class AppConfigurationProperties {

    private Endpoints endpoints = new Endpoints();
    private RestTemplateProps restTemplateProps = new RestTemplateProps();
    private List<String> languages = List.of("cs", "da", "de", "el", "en", "et", "fi", "fr", "hu", "it", "nl", "no", "pt", "sk", "sl", "sr", "sv");
    private OaiPmh oaiPmh = new OaiPmh();
    private Path baseDirectory = null;

    @Component
    @ConfigurationPropertiesBinding
    public static class PathConverter implements Converter<String, Path> {
        @Override
        public Path convert(@NonNull String s) {
            return Path.of(s).normalize();
        }
    }

    /**
     * Endpoints configuration model
     *
     * @author moses AT doraventures DOT com
     */
    @Data
    public static class Endpoints {
        private Map<String, Harvester> harvesters = Collections.emptyMap();
        private List<Repo> repos = Collections.emptyList();
    }

    /**
     * OaiPmh configuration model
     *
     * @author moses AT doraventures DOT com
     */
    @Data
    public static class OaiPmh {
      private MetadataParsingDefaultLang metadataParsingDefaultLang;
      private boolean concatRepeatedElements;
      private String concatSeparator;

        /**
         * Defaults for parsing metadata fields with no xml:lang specified,
         * where lang is extracted content is to be mapped against a lang
         *
         * @author moses AT doraventures DOT com
         */
        @Data
        public static class MetadataParsingDefaultLang {
          private boolean active;
          private String lang;
        }
    }
}
