/*
 * Copyright © 2017-2019 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.metrics;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.service.IngestService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.index.IndexNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implements metrics according to https://docs.google.com/spreadsheets/d/1vkqm-9sSHCgskRzKIz1R4B_8uzyJpjy1-rFooQyeDtg/edit
 */
@Component
@Slf4j
public class MicrometerMetrics {

    public static final String LIST_RECORD = "list.record";
    public static final String LIST_RECORD_LANGCODE = "list.record.langcode";

    private final AtomicLong totalRecords = new AtomicLong(0);
    private final Map<String, AtomicLong> recordsLanguagesMap = new ConcurrentHashMap<>();

    private final AppConfigurationProperties appConfigurationProperties;
    private final IngestService ingestService;

    @Autowired
    public MicrometerMetrics(AppConfigurationProperties appConfigurationProperties, IngestService ingestService, MeterRegistry meterRegistry) {
        this.appConfigurationProperties = appConfigurationProperties;
        this.ingestService = ingestService;

        // Total records metric
        Gauge.builder(LIST_RECORD, totalRecords::get)
                .description("Amount of records stored")
                .register(meterRegistry);

        // Records per language metric
        var languages = appConfigurationProperties.getLanguages();
        for (String language : languages) {
            recordsLanguagesMap.put(language, new AtomicLong());
            var builder = Gauge.builder(LIST_RECORD_LANGCODE, () -> getRecordCount(language));
            builder.description("Amount of records stored per language");
            builder.tag("langcode", language);
            builder.register(meterRegistry);
        }
    }

    /**
     * Gets the count of the amount of records currently stored in Elasticsearch.
     * Setting this {@link AtomicLong} will update the Micrometer metric.
     *
     * @param language the language to get
     * @throws IllegalArgumentException if the language is not configured
     */
    AtomicLong getRecordCount(String language) {
        AtomicLong languageRecords = recordsLanguagesMap.get(language);
        if (languageRecords != null) {
            return languageRecords;
        }
        throw new IllegalArgumentException(String.format("Invalid language code [%s]", language));
    }

    /**
     * Iterates through all configured languages and updates them
     *
     * @throws ElasticsearchException if Elasticsearch is unavailable
     */
    public void updateLanguageMetrics() {
        log.debug("Updating list_record_langcode metric.");
        for (var language : appConfigurationProperties.getLanguages()) {
            try {
                getRecordCount(language).set(ingestService.getTotalHitCount(language));
                log.trace("Language [{}] updated.", language);
            } catch (IndexNotFoundException e) {
                // There are no records in an index that does not exist
                getRecordCount(language).set(0);
                log.debug("Index not found for language [{}].", language);
            }
        }
    }

    /**
     * Updates the total records stored in Elasticsearch
     *
     * @throws ElasticsearchException if Elasticsearch is unavailable
     */
    public void updateTotalRecordsMetric() {
        log.debug("Updating list_records metric.");
        totalRecords.set(ingestService.getTotalHitCount());
    }

    AtomicLong getTotalRecords() {
        return totalRecords;
    }

    /**
     * Updates all configured metrics.
     *
     * @throws ElasticsearchException if Elasticsearch is unavailable
     */
    public void updateMetrics() {
        updateTotalRecordsMetric();
        updateLanguageMetrics();
    }
}
