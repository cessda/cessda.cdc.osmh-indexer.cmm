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

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.exception.IndexerException;
import eu.cessda.pasc.oci.models.Record;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.parser.RecordHeaderParser;
import eu.cessda.pasc.oci.parser.RecordXMLParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Optional;

/**
 * Manual Consumer test class this can be used to explore end to end behavior of this consumer and to some extend some
 * of the other components it interacts with:
 * <p>
 * Explore handler behavior for a known repo or how this consumer behaves in relation to this known repo.
 * Explore handler behavior for a new repo or how this consumer behaves in relation to this new repo.
 * <p>
 * Known repo= repo that has been tested and currently being consumed and index.
 *
 * @author moses AT doraventures DOT com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@Ignore("Ignoring: For manual Integration testing only")
@Slf4j
public class IndexerConsumerServiceRunnerTestIT {

    @Autowired
    private RecordHeaderParser recordHeaderParser;

    @Autowired
    private RecordXMLParser recordXMLParser;

    @Autowired
    private AppConfigurationProperties appConfigurationProperties;

    @Test
    @SuppressWarnings("java:S2699")
    public void shouldReturnASuccessfulResponseForAllConfiguredRepositories() throws IndexerException {

        var repos = appConfigurationProperties.getEndpoints().getRepos();
        var countReport = new HashMap<String, Integer>();
        for (var repo : repos) {
            countReport.put(repo.getCode(), processAndVerify(repo));
        }

        log.info("""

            #############################
            Printing Report for all repos
            #############################""");
        countReport.forEach((repo, headerCount) -> log.info("Header count for {}: [{}]", repo, headerCount));
        int sum = countReport.values().stream().reduce(0, Integer::sum);
        log.info("Total Count: [{}]", sum);
    }

    @SuppressWarnings("ReturnValueIgnored")
    private int processAndVerify(Repo repo) throws IndexerException {
        var recordHeaders = recordHeaderParser.getRecordHeaders(repo);
        log.info("Total records found: [{}]", recordHeaders.size());

        // We are only interested in the first valid record
        recordHeaders.stream().map(recordHeader -> {
            try {
                var record = recordXMLParser.getRecord(repo, new Record(recordHeader, null, null));
                log.info("|------------------------------Record Header----------------------------------------|");
                log.info(recordHeader.toString());
                log.info("|------------------------------Record CmmStudy--------------------------------------|");
                log.info(String.valueOf(record));
                return record;
            } catch (IndexerException e) {
                return Optional.empty();
            }
        }).filter(Optional::isPresent).findAny().orElseThrow(); // If no records can be retrieved, fail the test

        return recordHeaders.size();
    }
}