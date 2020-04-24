package eu.cessda.pasc.oci.metrics;

import org.elasticsearch.ElasticsearchException;

public interface Metrics {
    /**
     * Iterates through all configured languages and updates them.
     *
     * @throws ElasticsearchException if Elasticsearch is unavailable.
     */
    void updateLanguageMetrics();

    /**
     * Updates the total records stored in Elasticsearch.
     *
     * @throws ElasticsearchException if Elasticsearch is unavailable.
     */
    void updateTotalRecordsMetric();

    /**
     * Updates all configured metrics.
     *
     * @throws ElasticsearchException if Elasticsearch is unavailable.
     */
    void updateMetrics();
}
