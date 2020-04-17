package eu.cessda.pasc.oci.metrics;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.service.IngestService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
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

    public static final String LIST_RECORD_LANGCODE = "list.record.langcode";
    private final Map<String, AtomicLong> recordsLanguagesMap = new ConcurrentHashMap<>();

    private final AppConfigurationProperties appConfigurationProperties;
    private final IngestService ingestService;

    @Autowired
    public MicrometerMetrics(AppConfigurationProperties appConfigurationProperties, IngestService ingestService, MeterRegistry meterRegistry) {
        this.appConfigurationProperties = appConfigurationProperties;
        this.ingestService = ingestService;
        var languages = appConfigurationProperties.getLanguages();
        for (String language : languages) {
            recordsLanguagesMap.put(language, new AtomicLong());
            Gauge.builder(LIST_RECORD_LANGCODE, () -> getRecordCount(language))
                    .description("Amount of records stored per language")
                    .tag("langcode", language)
                    .register(meterRegistry);
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
}
