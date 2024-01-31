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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ElasticsearchConfigurationTest {

    private ElasticsearchConfiguration getElasticsearchConfiguration() {
        return new ElasticsearchConfiguration(
            "localhost",
            9200,
            null,
            null,
            new ObjectMapper()
        );
    }

    @Test
    public void shouldCreateElasticsearchRestClient() {
        // Given
        var elasticsearchConfiguration = getElasticsearchConfiguration();
        var transport = elasticsearchConfiguration.elasticsearchTransport();
        var client = elasticsearchConfiguration.elasticsearchClient(transport);

        // Then
        assertNotNull(client);
    }
}
