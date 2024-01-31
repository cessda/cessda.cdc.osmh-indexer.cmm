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
package eu.cessda.pasc.oci;

import eu.cessda.pasc.oci.service.DebuggingJMXBean;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static net.logstash.logback.argument.StructuredArguments.value;

/**
 * Service responsible for triggering Metadata Harvesting and ingestion into the search engine
 *
 * @author moses AT doraventures DOT com
 */
@Component
@Slf4j
public class ConsumerScheduler {

    private static final String FULL_RUN = "Full Run";

    // Logging fields
    private static final String INDEXER_JOB_ID = "indexer_job_id";

    private final DebuggingJMXBean debuggingJMXBean;
    private final IndexerRunner indexerRunner;

    @Autowired
    public ConsumerScheduler(DebuggingJMXBean debuggingJMXBean, IndexerRunner indexerRunner) {
        this.debuggingJMXBean = debuggingJMXBean;
        this.indexerRunner = indexerRunner;
    }

    /**
     * Auto Starts after delay of given time at startup.
     */
    public void runIndexer() {
        // Record the start time to generate the job ID and for logging purposes
        final var startTime = OffsetDateTime.now(ZoneId.systemDefault());
        try (var jobKeyClosable = MDC.putCloseable(
            INDEXER_JOB_ID,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(startTime)
        )) {
            log.info("[{}] Consume and Ingest All SPs Repos: \nStarted at [{}]\nCurrent state before run:\n{}",
                FULL_RUN,
                startTime,
                debuggingJMXBean.printElasticSearchInfo()
            );

            indexerRunner.executeHarvestAndIngest();

            final var endTime = OffsetDateTime.now(ZoneId.systemDefault());
            log.info("[{}] Consume and Ingest All SPs Repos:\nEnded at: [{}]\nDuration: [{}] seconds",
                FULL_RUN,
                endTime,
                value("job_duration", Duration.between(startTime, endTime).getSeconds())
            );
        } catch (IOException e) {
            log.error("Cannot connect to Elasticsearch: " + e);
        }
    }
}
