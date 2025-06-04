/*
 * Copyright © 2017-2025 CESSDA ERIC (support@cessda.eu)
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

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.configurations.ESConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@EnableConfigurationProperties({AppConfigurationProperties.class, ESConfigurationProperties.class})
@SpringBootApplication
@Slf4j
public class OCIApplication {

    private static int exitCode = 0;

	public static void main(String[] args) {
        // Start the application. This method returns once Runner.run() returns.
        var applicationContext = SpringApplication.run(OCIApplication.class, args);

        // Exit the Spring Boot application. The exit code will have already been set.
        var status = SpringApplication.exit(applicationContext, () -> exitCode);
        System.exit(status);
    }

    @Component
    @Profile("!test")
    @SuppressWarnings({"java:S3985", "UnusedNestedClass"})
    private static class Runner implements CommandLineRunner {
        private final ConsumerScheduler consumerScheduler;

        public Runner(ConsumerScheduler consumerScheduler) {
            this.consumerScheduler = consumerScheduler;
        }

        /**
         * Run the indexer. If an exception is thrown, the exit code of the indexer is set to -1.
         * @param args unused.
         */
        @Override
        @SuppressWarnings({"java:S1181", "java:S2696"}) // This is a top level error handler
        public void run(String... args) {
            try {
                consumerScheduler.runIndexer();
            } catch (Throwable e) {
                // Log all application errors, then exit with a non-zero exit code
                log.error("Fatal exception thrown!", e);
                exitCode = -1;
            }
        }
    }
}
