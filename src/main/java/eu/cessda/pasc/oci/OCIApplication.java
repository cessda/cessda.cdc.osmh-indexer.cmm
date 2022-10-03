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
package eu.cessda.pasc.oci;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.TimeZone;

@SpringBootApplication
@Slf4j
public class OCIApplication {

    private static int exitCode = 0;

    private final ConsumerScheduler consumerScheduler;

    public OCIApplication(ConsumerScheduler consumerScheduler) {
        this.consumerScheduler = consumerScheduler;
    }

	public static void main(String[] args) {
        // Set settings needed for the application to work properly
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // Start the application
        System.exit(SpringApplication.exit(SpringApplication.run(OCIApplication.class, args), () -> exitCode));
    }

    @Component
    @Profile("!test")
    @SuppressWarnings("java:S3985")
    private class Runner implements CommandLineRunner {
        /**
         * Run the indexer. If an exception is thrown, the exit code of the indexer is set to -1.
         * @param args unused.
         */
        @Override
        @SuppressWarnings({"java:S1181", "java:S2696"}) // This is a top level error handler
        public void run(String... args) {
            try {
                consumerScheduler.fullHarvestAndIngestionAllConfiguredSPsReposRecords();
            } catch (Throwable e) {
                // Log all application errors, then exit with a non-zero exit code
                log.error("Fatal exception thrown!", e);
                exitCode = -1;
            }
        }
    }
}
