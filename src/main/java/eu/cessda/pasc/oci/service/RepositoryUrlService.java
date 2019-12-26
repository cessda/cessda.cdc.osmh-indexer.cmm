package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.helpers.FakeHarvester;
import eu.cessda.pasc.oci.models.configurations.Repo;
import eu.cessda.pasc.oci.service.helpers.StudyIdentifierEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class RepositoryUrlService {

  private static final String LIST_RECORD_TEMPLATE = "%s/%s/ListRecordHeaders?Repository=%s";
  private static final String GET_RECORD_TEMPLATE = "%s/%s/GetRecord/CMMStudy/%s?Repository=%s";

  private final FakeHarvester fakeHarvester;
  private final AppConfigurationProperties appConfigurationProperties;


  @Autowired
  public RepositoryUrlService(FakeHarvester fakeHarvester, AppConfigurationProperties appConfigurationProperties) {
    this.fakeHarvester = fakeHarvester;
    this.appConfigurationProperties = appConfigurationProperties;
  }

  public String constructListRecordUrl(String repositoryUrl) {
    //pascOciConfig.getHarvesterUrl(),  Fixme: reeanable and remove the fake following urls

    String finalUrl = "replace_me";
    Optional<Repo> repoOptional = fakeHarvester.getRepoConfigurationProperties(repositoryUrl);

    if (repoOptional.isPresent()) {
      finalUrl = String.format(LIST_RECORD_TEMPLATE,
          repoOptional.get().getHandler(),
          appConfigurationProperties.getHarvester().getVersion(),
          repositoryUrl);
    }

    log.info("[{}] Final ListHeaders Handler url [{}] constructed.", repositoryUrl, finalUrl);
    return finalUrl;
  }

  public String constructGetRecordUrl(String repositoryUrl, String studyNumber) {
    //pascOciConfig.getHarvesterUrl(),  Fixme: reeanable and remove the fake following urls

    String finalUrl = "replace_me";
    Optional<Repo> repoOptional = fakeHarvester.getRepoConfigurationProperties(repositoryUrl);
    String encodedStudyID = StudyIdentifierEncoder.encodeStudyIdentifier().apply(studyNumber);

    if (repoOptional.isPresent()) {
      finalUrl = String.format(GET_RECORD_TEMPLATE,
          repoOptional.get().getHandler(),
          appConfigurationProperties.getHarvester().getVersion(),
          encodedStudyID,
          repositoryUrl);
    }
    log.trace("[{}] Final GetRecord Handler url [{}] constructed.", repositoryUrl, finalUrl);
    return finalUrl;
  }
}