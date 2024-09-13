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
package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.configurations.ElasticsearchConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class DebuggingJMXBeanTestIT {

    @Autowired
    private AppConfigurationProperties appConfigurationProperties;

    @Autowired
    private ElasticsearchConfiguration elasticsearchTemplate;

    @Test
    public void shouldPrintElasticsearchDetails() throws IOException {
        // Class under test
        DebuggingJMXBean debuggingJMXBean = new DebuggingJMXBean(elasticsearchTemplate.elasticsearchClient());
        assertThat(debuggingJMXBean.printElasticSearchInfo()).startsWith("Elasticsearch Client Settings");
    }
}
