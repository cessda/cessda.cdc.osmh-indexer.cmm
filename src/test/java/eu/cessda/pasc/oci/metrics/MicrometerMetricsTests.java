package eu.cessda.pasc.oci.metrics;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.service.IngestService;
import io.micrometer.core.instrument.MeterRegistry;
import org.elasticsearch.index.IndexNotFoundException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

/**
 * Tests metrics according to https://docs.google.com/spreadsheets/d/1vkqm-9sSHCgskRzKIz1R4B_8uzyJpjy1-rFooQyeDtg/edit
 */
public class MicrometerMetricsTests {

    private final AppConfigurationProperties appConfigurationProperties = Mockito.mock(AppConfigurationProperties.class);
    private final MeterRegistry meterRegistry = Mockito.mock(MeterRegistry.class);

    public MicrometerMetricsTests() {
        Mockito.doReturn(Arrays.asList("en", "de", "sv", "cz")).when(appConfigurationProperties).getLanguages();
    }

    @Test
    public void shouldReturnListOfRecordsByLanguage() {

        // When
        var micrometerMetrics = new MicrometerMetrics(appConfigurationProperties, null, meterRegistry);

        // All configured languages should be populated, no exception should be thrown
        for (var language : appConfigurationProperties.getLanguages()) {
            Assert.assertNotNull(micrometerMetrics.getRecordCount(language));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenAnInvalidLanguageIsSpecified() {

        // When
        var micrometerMetrics = new MicrometerMetrics(appConfigurationProperties, null, meterRegistry);

        micrometerMetrics.getRecordCount("moon");
    }


    @Test
    public void shouldContainCorrectAmountOfRecords() {
        IngestService ingestService = Mockito.mock(IngestService.class);

        // When
        long expected = 42;
        String language = "en";
        Mockito.when(ingestService.getTotalHitCount(Mockito.anyString())).thenReturn(0L);
        Mockito.when(ingestService.getTotalHitCount(language)).thenReturn(expected);

        var micrometerMetrics = new MicrometerMetrics(appConfigurationProperties, ingestService, meterRegistry);
        micrometerMetrics.updateLanguageMetrics();

        // The expected result should be the same as returned from Elasticsearch
        for (var configuredLanguage : appConfigurationProperties.getLanguages()) {
            if (configuredLanguage.equals(language)) {
                Assert.assertEquals(expected, micrometerMetrics.getRecordCount(configuredLanguage).get());
            } else {
                Assert.assertEquals(0, micrometerMetrics.getRecordCount(configuredLanguage).get());
            }
        }
    }

    @Test
    public void shouldSetRecordCountToZeroWhenIndexIsNotFound() {
        IngestService ingestService = Mockito.mock(IngestService.class);

        // When
        String language = "en";
        Mockito.when(ingestService.getTotalHitCount(Mockito.anyString())).thenThrow(new IndexNotFoundException(""));

        var micrometerMetrics = new MicrometerMetrics(appConfigurationProperties, ingestService, meterRegistry);
        micrometerMetrics.getRecordCount(language).set(23);

        micrometerMetrics.updateLanguageMetrics();

        Assert.assertEquals(0L, micrometerMetrics.getRecordCount(language).get());
    }
}
