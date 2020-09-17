/*
 * Copyright © 2017-2020 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci;

import eu.cessda.pasc.oci.metrics.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableMBeanExport
@EnableScheduling
@Slf4j
public class OCIApplication {

    private final Metrics metrics;

	public OCIApplication(Metrics metrics) {
		this.metrics = metrics;
	}

	public static void main(String[] args) {
        // Set settings needed for the application to work properly
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // Start the application
        SpringApplication.run(OCIApplication.class, args);
    }

	@EventListener
	public void startupMetrics(ContextRefreshedEvent contextRefreshedEvent) {
		log.debug("Setting metrics");
		try {
			metrics.updateLanguageMetrics();
			metrics.updateTotalRecordsMetric();
		} catch (ElasticsearchException e) {
			log.warn("Couldn't initialise metrics on startup. \n{}", e.toString());
		}
	}
}
