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
package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class DebuggingJMXBeanTestIT {

    // Class under test
    private DebuggingJMXBean debuggingJMXBean;

    @Autowired
    private AppConfigurationProperties appConfigurationProperties;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchTemplate;

    @Test
    public void shouldPrintElasticsearchDetails() {
        debuggingJMXBean = new DebuggingJMXBean(elasticsearchTemplate, appConfigurationProperties);
        assertThat(debuggingJMXBean.printElasticSearchInfo()).startsWith("Elasticsearch Client Settings");
    }

    @Test
    public void shouldPrintCurrentlyConfiguredRepoEndpoints() {
        debuggingJMXBean = new DebuggingJMXBean(elasticsearchTemplate, appConfigurationProperties);

        // When
        String actualRepos = debuggingJMXBean.printCurrentlyConfiguredRepoEndpoints();
        assertThat(actualRepos).isNotEmpty();
    }
}
