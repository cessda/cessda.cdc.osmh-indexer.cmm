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
package eu.cessda.pasc.oci.metrics;

import eu.cessda.pasc.oci.LoggingConstants;
import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.elasticsearch.IngestService;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.configurations.Repo;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.index.IndexNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Implements metrics according to https://docs.google.com/spreadsheets/d/1vkqm-9sSHCgskRzKIz1R4B_8uzyJpjy1-rFooQyeDtg/edit
 *
 * @author Matthew Morris
 */
@Component
@Slf4j
public class MicrometerMetrics implements Metrics {

    // Defines the metric names
    private static final String NUM_RECORDS_HARVESTED = "num.records.harvested";
    private static final String LIST_RECORD_LANGCODE = "list.record.langcode";
    private static final String LIST_RECORDS_ENDPOINT = "list.records.endpoint";

    // For logging
    private static final String UPDATING_METRIC = "Updating {} metric.";

    // Stores the metrics
    private final AtomicLong totalRecords = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> recordsLanguagesMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Repo, AtomicLong> recordsEndpointMap = new ConcurrentHashMap<>();

    private final AppConfigurationProperties appConfigurationProperties;
    private final IngestService ingestService;

    @Autowired
    public MicrometerMetrics(AppConfigurationProperties appConfigurationProperties, IngestService ingestService, MeterRegistry meterRegistry) {
        this.appConfigurationProperties = appConfigurationProperties;
        this.ingestService = ingestService;

        // Total records metric
        Gauge.builder(NUM_RECORDS_HARVESTED, totalRecords::get)
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

        // Records per endpoint metric
        var endpoints = appConfigurationProperties.getEndpoints().getRepos();
        for (Repo endpoint : endpoints) {
            recordsEndpointMap.put(endpoint, new AtomicLong());
            var builder = Gauge.builder(LIST_RECORDS_ENDPOINT, () -> getRecordCount(endpoint));
            builder.description("Amount of records stored per endpoint");
            builder.tag("endpoint", endpoint.getUrl().toString()); // Should this be the short name
            builder.register(meterRegistry);
        }
    }

    /**
     * Gets the count of the amount of records currently stored in Elasticsearch.
     * Setting this {@link AtomicLong} will update the Micrometer metric.
     *
     * @param language the language to get
     * @throws IllegalArgumentException if the language is not configured.
     */
    AtomicLong getRecordCount(String language) {
        AtomicLong languageRecords = recordsLanguagesMap.get(language);
        if (languageRecords != null) {
            return languageRecords;
        }
        throw new IllegalArgumentException(String.format("Invalid language code [%s]", language));
    }

    /**
     * {@inheritDoc}
     *
     * @throws ElasticsearchException if Elasticsearch is unavailable.
     */
    @Override
    public void updateLanguageMetrics() {
        log.debug(UPDATING_METRIC, LIST_RECORD_LANGCODE);
        for (var language : appConfigurationProperties.getLanguages()) {
            try {
                long totalHitCount = ingestService.getTotalHitCount(language);
                getRecordCount(language).set(totalHitCount);
                log.info("[{}] Current records [{}]", StructuredArguments.value(LoggingConstants.LANG_CODE, language),
                    StructuredArguments.value("language_record_count", totalHitCount));
            } catch (IndexNotFoundException e) {
                // There are no records in an index that does not exist
                getRecordCount(language).set(0);
                log.debug("Index not found for language [{}].", language);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws ElasticsearchException if Elasticsearch is unavailable.
     */
    @Override
    public void updateTotalRecordsMetric() {
        log.debug(UPDATING_METRIC, NUM_RECORDS_HARVESTED);
        totalRecords.set(ingestService.getTotalHitCount("*"));
    }

    AtomicLong getTotalRecords() {
        return totalRecords;
    }

    /**
     * Gets the count of the amount of records currently stored in Elasticsearch.
     * Setting this {@link AtomicLong} will update the Micrometer metric.
     *
     * @param repository the repository to get
     * @throws IllegalArgumentException if the repository is not configured
     */
    AtomicLong getRecordCount(Repo repository) {
        AtomicLong endpointRecord = recordsEndpointMap.get(repository);
        if (endpointRecord != null) {
            return endpointRecord;
        }
        throw new IllegalArgumentException(String.format("Invalid repository [%s]", repository.getCode()));
    }

    /**
     * Updates the total records stored in each repository.
     *
     * @param studies a collection of studies to analyse for endpoints.
     * @throws ElasticsearchException if Elasticsearch is unavailable.
     */
    void updateEndpointsRecordsMetric(Collection<CMMStudyOfLanguage> studies) {
        log.debug(UPDATING_METRIC, LIST_RECORDS_ENDPOINT);

        var hostEntryMap = studies.stream().filter(study -> study.getStudyXmlSourceUrl() != null)
            .map(study -> URI.create(study.getStudyXmlSourceUrl()))
            .collect(Collectors.groupingBy(URI::getHost, Collectors.counting()));

        for (Map.Entry<String, Long> hostEntry : hostEntryMap.entrySet()) {
            recordsEndpointMap.entrySet().stream()
                .filter(repoEntry -> repoEntry.getKey().getUrl().getHost().equalsIgnoreCase(hostEntry.getKey()))
                .findAny().ifPresentOrElse(repoAtomicLongEntry -> {
                    repoAtomicLongEntry.getValue().set(hostEntry.getValue());
                    log.info("[{}] Current records: [{}]",
                        StructuredArguments.value(LoggingConstants.REPO_NAME, repoAtomicLongEntry.getKey().getCode()),
                        StructuredArguments.value("repo_record_count", hostEntry.getValue()));
                }, () -> log.warn("Repository [{}] not configured.", hostEntry.getKey())
            );
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws ElasticsearchException if Elasticsearch is unavailable.
     */
    @Override
    public void updateMetrics() {
        updateTotalRecordsMetric();
        updateLanguageMetrics();
        var allStudies = ingestService.getAllStudies("*");
        updateEndpointsRecordsMetric(allStudies);
    }
}
