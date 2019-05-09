package eu.cessda.pasc.oci.repository;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.helpers.FakeHarvester;
import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;
import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static eu.cessda.pasc.oci.repository.StudyIdentifierEncoder.encodeStudyIdentifier;

/**
 * A Pasc Harvester Service implementation
 *
 * @author moses@doraventures.com
 */
@Service
@Slf4j
public class PascHarvesterDao extends DaoBase implements HarvesterDao {

  private static final String LIST_RECORD_TEMPLATE = "%s/%s/ListRecordHeaders?Repository=%s";
  private static final String GET_RECORD_TEMPLATE = "%s/%s/GetRecord/CMMStudy/%s?Repository=%s";

  @Autowired
  private FakeHarvester fakeHarvester;

  @Autowired
  private AppConfigurationProperties appConfigurationProperties;

  @Override
  public String listRecordHeaders(String spRepository) throws ExternalSystemException {

    String finalUrl = constructListRecordUrl(spRepository);
    return postForStringResponse(finalUrl);
  }

  @Override
  public String getRecord(String spRepository, String studyNumber) throws ExternalSystemException {
    String finalUrl = constructGetRecordUrl(spRepository, studyNumber);
    return postForStringResponse(finalUrl);
  }

  private List<String> constructListRecordUrl(String repositoryUrl) {
    //pascOciConfig.getHarvesterUrl(),  Fixme: re-enable and remove the fake following urls

    List<Repo> repos = fakeHarvester.getRepoConfigurationProperties(repositoryUrl);
    List<String> formattedListRecordUrls = new ArrayList<>(); // FIXME: Make this a Map<String, List<String>>.
    for (Repo repo : repos) {
      formattedListRecordUrls.add(
          String.format(LIST_RECORD_TEMPLATE, repo.getHandler(),
              appConfigurationProperties.getHarvester().getVersion(), repositoryUrl));
    }

    if (log.isInfoEnabled()) {
      String msgTemplate = "[{}] result to [{}] ListHeaders Handler url(s) [{}]";
      log.info(msgTemplate, repositoryUrl, formattedListRecordUrls.size(),
          formattedListRecordUrls
              .stream()
              .map(String::valueOf)
              .collect(Collectors.joining(", ")));
    }

    return formattedListRecordUrls;
  }

  private String constructGetRecordUrl(String repositoryUrl, String studyNumber) {
    //pascOciConfig.getHarvesterUrl(),  Fixme: re-enable and remove the fake following urls

    String finalUrl = "replace_me";
    Optional<Repo> repoOptional = fakeHarvester.getRepoConfigurationProperties(repositoryUrl);
    String encodedStudyID = encodeStudyIdentifier().apply(studyNumber);

    if (repoOptional.isPresent()) {
      finalUrl = String.format(GET_RECORD_TEMPLATE,
          repoOptional.get().getHandler(),
          appConfigurationProperties.getHarvester().getVersion(),
          encodedStudyID,
          repositoryUrl);
    }
    log.trace("[{}] Final GetRecord Handler url [{}]", repositoryUrl, finalUrl);
    return finalUrl;
  }
}
