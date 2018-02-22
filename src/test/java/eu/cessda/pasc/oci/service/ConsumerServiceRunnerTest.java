package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.models.RecordHeader;
import eu.cessda.pasc.oci.models.cmmstudy.CMMConverter;
import eu.cessda.pasc.oci.models.cmmstudy.CMMStudy;
import eu.cessda.pasc.oci.models.configurations.Repo;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static eu.cessda.pasc.oci.data.ReposTestData.getUKDSRepo;

/**
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = "report-subscriber=tester@example.com")
@Ignore("Ignoring: For manual Integration testing only")
public class ConsumerServiceRunnerTest {

  @Autowired
  DefaultConsumerService consumerService;

  @Test
  public void shouldReturnASuccessfulResponse() {

    Repo repo = getUKDSRepo();
    List<RecordHeader> recordHeaders = consumerService.listRecorderHeadersBody(repo);

//    assertThat(recordHeaders).hasSize(2);
    List<RecordHeader> top3RecordHeaders = recordHeaders.stream().skip(1000).limit(3).collect(Collectors.toList());
    top3RecordHeaders.forEach(recordHeader -> {
      System.out.println("|----------------------------------------------------------------------|");
      System.out.println("|--- Record Header");
      System.out.println(recordHeader);

      System.out.println("|--- Record CmmStudy");
      Optional<CMMStudy> optionalCmmStudy = consumerService.getRecord(repo, recordHeader.getIdentifier());
      optionalCmmStudy.ifPresent(s -> {
        CMMConverter.toJsonString(s)
            .ifPresent(System.out::println);
      });
    });
  }
}