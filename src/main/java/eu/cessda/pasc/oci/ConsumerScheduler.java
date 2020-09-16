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

import eu.cessda.pasc.oci.elasticsearch.IngestService;
import eu.cessda.pasc.oci.service.DebuggingJMXBean;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
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
@ManagedResource
@Slf4j
public class ConsumerScheduler {

    private static final String FULL_RUN = "Full Run";
    private static final String DAILY_INCREMENTAL_RUN = "Daily Incremental Run";

    // Logging fields
    private static final String INDEXER_JOB_ID = "indexer_job_id";
    private static final String RUN_TYPE = "run_type";

    private final DebuggingJMXBean debuggingJMXBean;
    private final IngestService esIndexerService;
    private final HarvesterRunner harvesterRunner;

    @Autowired
    public ConsumerScheduler(DebuggingJMXBean debuggingJMXBean, IngestService esIndexerService, HarvesterRunner harvesterRunner) {
        this.debuggingJMXBean = debuggingJMXBean;
        this.esIndexerService = esIndexerService;
        this.harvesterRunner = harvesterRunner;
    }

    /**
     * Auto Starts after delay of given time at startup.
     */
    @Async
    @ManagedOperation(description = "Manual trigger to do a full harvest and ingest run")
    @Scheduled(initialDelayString = "${osmhConsumer.delay.initial}", fixedDelayString = "${osmhConsumer.delay.fixed}")
    @SuppressWarnings("try")
    public void fullHarvestAndIngestionAllConfiguredSPsReposRecords() {
        try (var jobKeyClosable = MDC.putCloseable(RUN_TYPE, FULL_RUN)) {
            run(FULL_RUN, null);
      }
  }

    /**
     * Daily Harvest and Ingestion run.
     */
    @Async
    @ManagedOperation(description = "Manual trigger to do an incremental harvest and ingest")
    @Scheduled(cron = "${osmhConsumer.daily.run}")
    @SuppressWarnings("try")
    public void dailyIncrementalHarvestAndIngestionAllConfiguredSPsReposRecords() {
        try (var jobKeyClosable = MDC.putCloseable(RUN_TYPE, DAILY_INCREMENTAL_RUN)) {
            run(DAILY_INCREMENTAL_RUN, esIndexerService.getMostRecentLastModified().orElse(null));
        }
    }

    /**
     * Runs the harvest.
     *
     * @param runType     the run type, used for logging
     * @param harvestFrom the LocalDateTime to harvest from, where null is a full harvest
     */
    @SuppressWarnings("try")
    private void run(String runType, LocalDateTime harvestFrom) {
        try (var jobKeyClosable = MDC.putCloseable(INDEXER_JOB_ID, getJobId())) {
            final var startTime = logStartStatus(runType);
            harvesterRunner.executeHarvestAndIngest(harvestFrom);
            logEndStatus(startTime, runType);
        }
    }

    /**
     * Weekly run.
     */
    @Scheduled(cron = "${osmhConsumer.daily.sunday.run}")
    public void weeklyFullHarvestAndIngestionAllConfiguredSPsReposRecords() {
        log.info("Once a Week Full Run. Triggered by cron - STARTED");
        fullHarvestAndIngestionAllConfiguredSPsReposRecords();
        log.info("Once a Week Full Run. Triggered by cron - ENDED");
    }

    /**
     * Gets the correlation id of this run
     *
     * @return the correlation id
     */
    private String getJobId() {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now(ZoneId.systemDefault()));
    }

    private OffsetDateTime logStartStatus(final String runDescription) {
        final OffsetDateTime startTime = OffsetDateTime.now(ZoneId.systemDefault());
        log.info("[{}] Consume and Ingest All SPs Repos: \n" +
                "Started at [{}]\n" +
                "Current state before run:\n" +
                "{}\n" +
                "{}",
            runDescription, startTime, debuggingJMXBean.printCurrentlyConfiguredRepoEndpoints(),
            debuggingJMXBean.printElasticSearchInfo());
        return startTime;
    }

    private void logEndStatus(final OffsetDateTime startTime, final String runDescription) {
        final var endTime = OffsetDateTime.now(ZoneId.systemDefault());
        log.info("[{}] Consume and Ingest All SPs Repos:\n" +
                "Ended at: [{}]\n" +
                "Duration: [{}] seconds",
            runDescription,
            endTime,
            value("job_duration", Duration.between(startTime, endTime).getSeconds())
        );
    }
}
