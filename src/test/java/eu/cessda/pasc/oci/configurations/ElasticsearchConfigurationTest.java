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

import org.junit.Test;

import java.net.UnknownHostException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class ElasticsearchConfigurationTest {

    @Test
    public void shouldCreateElasticsearchClient() throws UnknownHostException {
        // Given
        var elasticsearchConfiguration = new ElasticsearchConfiguration();
        var client = elasticsearchConfiguration.client();

        // Then
        assertNotNull(client);

        // Close
        elasticsearchConfiguration.close();
        elasticsearchConfiguration.close(); // Should not throw on repeated calls
    }

    @Test
    public void shouldReturnExistingElasticsearchClient() throws UnknownHostException {
        try (var elasticsearchConfiguration = new ElasticsearchConfiguration()) {

            // Given
            var firstESClient = elasticsearchConfiguration.client();
            var secondESClient = elasticsearchConfiguration.client();

            // Then
            assertSame(firstESClient, secondESClient);
        }
    }

    @Test
    public void shouldReturnElasticsearchTemplate() throws UnknownHostException {
        try (var elasticsearchConfiguration = new ElasticsearchConfiguration()) {

            // Given
            var esTemplate = elasticsearchConfiguration.elasticsearchTemplate();

            // Then
            assertNotNull(esTemplate);
        }
    }
}