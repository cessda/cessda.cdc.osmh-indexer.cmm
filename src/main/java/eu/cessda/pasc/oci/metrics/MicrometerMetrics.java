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
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudyOfLanguage;
import eu.cessda.pasc.oci.models.cmmstudy.Publisher;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.service.IngestService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.index.IndexNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implements metrics according to https://docs.google.com/spreadsheets/d/1vkqm-9sSHCgskRzKIz1R4B_8uzyJpjy1-rFooQyeDtg/edit
 *
 * @author Matthew Morris
 */
@Component
@Slf4j
public class MicrometerMetrics {

    // Defines the metric names
    public static final String NUM_RECORDS_HARVESTED = "num.records.harvested";
    public static final String LIST_RECORD_LANGCODE = "list.record.langcode";
    private static final String LIST_RECORDS_ENDPOINT = "list.records.endpoint";
    private static final String LIST_RECORDS_PUBLISHER = "list.records.publisher";

    // For logging
    private static final String UPDATING_METRIC = "Updating {} metric.";

    // Stores the metrics
    private final AtomicLong totalRecords = new AtomicLong(0);
    private final ConcurrentHashMap<String, AtomicLong> recordsLanguagesMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Repo, AtomicLong> recordsEndpointMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Publisher, AtomicLong> recordsPublisherMap = new ConcurrentHashMap<>();

    private final AppConfigurationProperties appConfigurationProperties;
    private final IngestService ingestService;
    private final MeterRegistry meterRegistry;

    @Autowired
    public MicrometerMetrics(AppConfigurationProperties appConfigurationProperties, IngestService ingestService, MeterRegistry meterRegistry) {
        this.appConfigurationProperties = appConfigurationProperties;
        this.ingestService = ingestService;
        this.meterRegistry = meterRegistry;

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
     * Iterates through all configured languages and updates them.
     *
     * @throws ElasticsearchException if Elasticsearch is unavailable.
     */
    public void updateLanguageMetrics() {
        log.debug(UPDATING_METRIC, LIST_RECORD_LANGCODE);
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
     * Updates the total records stored in Elasticsearch.
     *
     * @throws ElasticsearchException if Elasticsearch is unavailable.
     */
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
        throw new IllegalArgumentException(String.format("Invalid repository [%s]", repository.getName()));
    }

    /**
     * Updates the total records stored in each repository.
     *
     * @throws ElasticsearchException if Elasticsearch is unavailable.
     */
    public void updateEndpointsRecordsMetric(Set<CMMStudyOfLanguage> studies) {
        log.debug(UPDATING_METRIC, LIST_RECORDS_ENDPOINT);
        var hitCountPerRepository = new HashMap<String, AtomicInteger>();

        for (CMMStudyOfLanguage study : studies) {
            if (study.getStudyXmlSourceUrl() != null) {
                String host = URI.create(study.getStudyXmlSourceUrl()).getHost();
                hitCountPerRepository.computeIfAbsent(host, k -> new AtomicInteger(0)).getAndIncrement();
            }
        }

        for (Map.Entry<String, AtomicInteger> hostEntry : hitCountPerRepository.entrySet()) {
            try {
                var repoAtomicLongEntry = recordsEndpointMap.entrySet().stream()
                        .filter(repoEntry -> repoEntry.getKey().getUrl().getHost().equalsIgnoreCase(hostEntry.getKey()))
                        .findAny().orElseThrow();
                log.trace("Repository [{}] updated", repoAtomicLongEntry.getKey().getName());
                repoAtomicLongEntry.getValue().set(hostEntry.getValue().get());
            } catch (NoSuchElementException e) {
                log.warn("Repository corresponding to [{}] not configured.", hostEntry.getKey());
            }
        }
    }

    /**
     * Updates the total records stored for each publisher.
     *
     * @throws ElasticsearchException if Elasticsearch is unavailable.
     */
    public void updatePublisherRecordsMetric(Set<CMMStudyOfLanguage> studies) {
        log.debug(UPDATING_METRIC, LIST_RECORDS_PUBLISHER);
        var hitCountPerPublisher = new HashMap<Publisher, AtomicInteger>();

        for (CMMStudyOfLanguage study : studies) {
            hitCountPerPublisher.computeIfAbsent(study.getPublisher(), p -> new AtomicInteger(0)).getAndIncrement();
        }

        hitCountPerPublisher.forEach((publisherKey, counter) ->
                // This registers the metrics for each publisher on first encounter
                recordsPublisherMap.computeIfAbsent(publisherKey, publisher -> {
                    var builder = Gauge.builder(LIST_RECORDS_PUBLISHER, () -> getRecordCount(publisher));
                    builder.description("Amount of records stored per publisher");
                    builder.tag("publisher", publisher.getPublisher());
                    builder.register(meterRegistry);
                    return new AtomicLong(0);
                }).set(counter.get())
        );
    }

    /**
     * Gets the count of the amount of records currently stored in Elasticsearch.
     * Setting this {@link AtomicLong} will update the Micrometer metric.
     *
     * @param publisher the publisher to get.
     * @throws IllegalArgumentException if the publisher is not stored in the map.
     */
    AtomicLong getRecordCount(Publisher publisher) {
        AtomicLong publisherRecord = recordsPublisherMap.get(publisher);
        if (publisherRecord != null) {
            return publisherRecord;
        }
        throw new IllegalArgumentException(String.format("Invalid publisher [%s]", publisher.getPublisher()));
    }

    /**
     * Updates all configured metrics.
     *
     * @throws ElasticsearchException if Elasticsearch is unavailable.
     */
    public void updateMetrics() {
        updateTotalRecordsMetric();
        updateLanguageMetrics();
        Set<CMMStudyOfLanguage> allStudies = ingestService.getAllStudies("*");
        updateEndpointsRecordsMetric(allStudies);
        updatePublisherRecordsMetric(allStudies);
    }
}
