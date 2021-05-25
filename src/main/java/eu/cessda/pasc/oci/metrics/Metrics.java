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

import org.elasticsearch.ElasticsearchException;

import java.io.IOException;

public interface Metrics {
    /**
     * Iterates through all configured languages and updates them.
     *
     * @throws ElasticsearchException if an error occurs in Elasticsearch.
     * @throws IOException if Elasticsearch cannot be contacted.
     */
    void updateLanguageMetrics() throws IOException;

    /**
     * Updates the total records stored in Elasticsearch.
     *
     * @throws ElasticsearchException if an error occurs in Elasticsearch.
     * @throws IOException if Elasticsearch cannot be contacted.
     */
    void updateTotalRecordsMetric() throws IOException;

    /**
     * Updates all configured metrics.
     *
     * @throws ElasticsearchException if an error occurs in Elasticsearch.
     * @throws IOException if Elasticsearch cannot be contacted.
     */
    void updateMetrics() throws IOException;
}
