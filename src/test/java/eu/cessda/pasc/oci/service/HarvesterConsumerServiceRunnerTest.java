package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static eu.cessda.pasc.oci.data.ReposTestData.*;
import static java.lang.String.format;
import static org.assertj.core.api.Java6BDDAssertions.then;

/**
 * Manual Consumer test class this can be used to explore end to end behavior of this consumer and to some extend some
 * of the other components it interacts with:
 * <p>
 * Explore handler behavior for a known repo or how this consumer behaves in relation to this known repo.
 * Explore handler behavior for a new repo or how this consumer behaves in relation to this new repo.
 * <p>
 * Known repo= repo that has been tested and currently being consumed and index.
 *
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("dev")
@Ignore("Ignoring: For manual Integration testing only")
@Slf4j
public class HarvesterConsumerServiceRunnerTest {

  @Autowired
  HarvesterConsumerService harvesterConsumerService;

  @Test
  public void shouldReturnASuccessfulResponseForUKDS() {

    Map<String, Integer> countReport = new HashMap<>();
    countReport.putAll(processAndVerify(getUKDSRepo()));
    countReport.putAll(processAndVerify(getGesisEnRepo()));
    countReport.putAll(processAndVerify(getGesisDeRepo()));

    System.out.println("############################################################################################");
    System.out.println("Printing Report for all repos");
    System.out.println("############################################################################################");
    countReport.forEach((repo, headerCount) -> System.out.println("#### " + headerCount + " Header count for " + repo));
    long sum = countReport.entrySet().stream().mapToLong(Map.Entry::getValue).sum();
    System.out.println("#### Total Count : " + sum);
    System.out.println("############################################################################################");
  }

  private Map<String, Integer> processAndVerify(Repo repo) {
    List<RecordHeader> recordHeaders = harvesterConsumerService.listRecordHeaders(repo, null);
    System.out.println("############################################################################################");
    int size = recordHeaders.size();
    System.out.println(format("Total records found [%s]", size));
    System.out.println("############################################################################################");
    List<RecordHeader> top3RecordHeaders = recordHeaders.stream().skip(1000).limit(3).collect(Collectors.toList());

    then(top3RecordHeaders).hasSize(3);

    top3RecordHeaders.forEach(recordHeader -> {
      log.info("|------------------------------Record Header----------------------------------------|");
      log.info(recordHeader.toString());

      log.info("|------------------------------Record CmmStudy----------------------------------------|");
      Optional<CMMStudy> optionalCmmStudy = harvesterConsumerService.getRecord(repo, recordHeader.getIdentifier());
      then(optionalCmmStudy.isPresent()).isTrue();
    });

    Map<String, Integer> repoHeadersCount = new HashMap<>();
    repoHeadersCount.put(repo.getName(), size);

    return repoHeadersCount;
  }
}