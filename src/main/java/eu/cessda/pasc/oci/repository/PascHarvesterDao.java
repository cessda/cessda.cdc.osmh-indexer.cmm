package eu.cessda.pasc.oci.repository;

import eu.cessda.pasc.oci.configurations.PaSCOciConfigurationProperties;
import eu.cessda.pasc.oci.helpers.FakeHarvester;
import eu.cessda.pasc.oci.helpers.exception.ExternalSystemException;
import eu.cessda.pasc.oci.models.configurations.Repo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * A Pasc Harvester Service implementation
 *
 * @author moses@doraventures.com
 */
@Service
public class PascHarvesterDao extends DaoBase implements HarvesterDao {

  private static final String LIST_RECORD_TEMPLATE = "%s/%s/ListRecordHeaders?Repository=%s";
  private static final String GET_RECORD_TEMPLATE = "%s/%s/GetRecord/CMMStudy/%s?Repository=%s";

  @Autowired
  private FakeHarvester fakeHarvester;

  @Autowired
  private PaSCOciConfigurationProperties paSCOciConfigurationProperties;

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

  private String constructListRecordUrl(String repositoryUrl) {
    Optional<Repo> tempDirectHandlerUrlInPlaceForHarvester = fakeHarvester.getUrlToCall(repositoryUrl);
    if (tempDirectHandlerUrlInPlaceForHarvester.isPresent()) {
      return String.format(LIST_RECORD_TEMPLATE,
          //pascOciConfig.getHarvesterUrl(),  Fixme: reeanable and remove the fake following urls
          tempDirectHandlerUrlInPlaceForHarvester.get().getHandler(),
          paSCOciConfigurationProperties.getHarvester().getVersion(),
          repositoryUrl);
    }
    return "";
  }

  private String constructGetRecordUrl(String repositoryUrl, String studyNumber) {
    Optional<Repo> tempDirectHandlerUrlInPlaceForHarvester = fakeHarvester.getUrlToCall(repositoryUrl);
    if (tempDirectHandlerUrlInPlaceForHarvester.isPresent()) {
      return String.format(GET_RECORD_TEMPLATE,
          //pascOciConfig.getHarvesterUrl(),  Fixme: reeanable and remove the fake following urls
          tempDirectHandlerUrlInPlaceForHarvester.get().getHandler(),
          paSCOciConfigurationProperties.getHarvester().getVersion(),
          studyNumber,
          repositoryUrl);
    }
    return "";
  }
}
