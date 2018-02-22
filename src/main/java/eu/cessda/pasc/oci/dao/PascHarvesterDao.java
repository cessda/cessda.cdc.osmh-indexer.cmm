package eu.cessda.pasc.oci.dao;

import eu.cessda.pasc.oci.configurations.PascOciConfig;
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

  @Autowired
  private FakeHarvester fakeHarvester;

  @Autowired
  private PascOciConfig pascOciConfig;

  @Override
  public String listRecordHeaders(String repositoryUrl) throws ExternalSystemException {

    String finalUrl = constructListRecordUrl(repositoryUrl);
    return postForStringResponse(finalUrl);
  }

  private String constructListRecordUrl(String repositoryUrl) {
    Optional<Repo> tempDirectHandlerUrlInPlaceForHarvester = fakeHarvester.getUrlToCall(repositoryUrl);
    if (tempDirectHandlerUrlInPlaceForHarvester.isPresent()) {
      return String.format(LIST_RECORD_TEMPLATE,
          //pascOciConfig.getHarvesterUrl(),  Fixme: reeanable and remote the fake following url.s
          tempDirectHandlerUrlInPlaceForHarvester.get().getHandler(),
          pascOciConfig.getHarvester().getVersion(),
          repositoryUrl);
    }
    return "";
  }
}
