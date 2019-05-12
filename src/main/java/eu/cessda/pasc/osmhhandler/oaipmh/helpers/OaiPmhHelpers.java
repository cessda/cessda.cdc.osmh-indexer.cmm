package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.exception.CustomHandlerException;
import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.OaiPmh;
import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.Repo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.GET_RECORD_URL_TEMPLATE;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.GET_RECORD_VALUE;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.IDENTIFIER_PARAM_KEY;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.LIST_IDENTIFIERS_VALUE;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.LIST_RECORD_HEADERS_PER_SET_URL_TEMPLATE;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.LIST_RECORD_HEADERS_URL_TEMPLATE;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.METADATA_PREFIX_PARAM_KEY;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.RESUMPTION_TOKEN_KEY;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.SET_SPEC_PARAM_KEY;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.VERB_PARAM_KEY;

/**
 * Helper methods to deal with Oai-pmh protocol
 *
 * @author moses@doraventures.com
 */
@Slf4j
public class OaiPmhHelpers {

  private OaiPmhHelpers() {
    throw new UnsupportedOperationException("Utility class, instantiation not allow");
  }

  public static String appendListRecordParams(String repoUrl, OaiPmh oaiPmh) throws CustomHandlerException {
    Repo repoConfig = getMetadataPrefix(repoUrl, oaiPmh);

    if (StringUtils.isBlank(repoConfig.getSetSpec())) {
      return String.format(LIST_RECORD_HEADERS_URL_TEMPLATE, repoUrl,
          VERB_PARAM_KEY, LIST_IDENTIFIERS_VALUE, // verb=ListIdentifier
          METADATA_PREFIX_PARAM_KEY, repoConfig.getPreferredMetadataParam()); //&metadataPrefix=ddi
    } else {
      return String.format(LIST_RECORD_HEADERS_PER_SET_URL_TEMPLATE, repoUrl,
          VERB_PARAM_KEY, LIST_IDENTIFIERS_VALUE, // verb=ListIdentifier
          METADATA_PREFIX_PARAM_KEY, repoConfig.getPreferredMetadataParam(), //&metadataPrefix=ddi
          SET_SPEC_PARAM_KEY, repoConfig.getSetSpec()); //&set=my:set
    }
  }

  public static String appendListRecordResumptionToken(String baseRepoUrl, String resumptionToken) {
    return String.format(LIST_RECORD_HEADERS_URL_TEMPLATE, baseRepoUrl,
        VERB_PARAM_KEY, LIST_IDENTIFIERS_VALUE, // verb=ListIdentifier
        RESUMPTION_TOKEN_KEY, resumptionToken // &resumptionToken=0001/500....
    );
  }

  public static String appendGetRecordParams(String repoUrl, String identifier, OaiPmh oaiPmh) throws CustomHandlerException {
    return String.format(
        GET_RECORD_URL_TEMPLATE, repoUrl,
        VERB_PARAM_KEY, GET_RECORD_VALUE, // verb=GetRecord
        IDENTIFIER_PARAM_KEY, identifier, //&identifier=1683
        METADATA_PREFIX_PARAM_KEY, getMetadataPrefix(repoUrl, oaiPmh).getPreferredMetadataParam() //&metadataPrefix=ddi
    );
  }

  public static String decodeStudyNumber(String encodedStudyNumber) {
    return encodedStudyNumber.replaceAll("_dt_", ".")
        .replaceAll("_sl_", "/")
        .replaceAll("_cl_", ":");
  }

  private static Repo getMetadataPrefix(String repoUrl, OaiPmh oaiPmh) throws CustomHandlerException {

    Repo repo = oaiPmh.getRepos()
        .stream()
        .filter(currentStreamRepo -> currentStreamRepo.getUrl().equalsIgnoreCase(repoUrl))
        .findFirst()
        .orElseThrow(() -> new CustomHandlerException(String.format("Configuration not found for Repo [%s]", repoUrl)));

    log.debug("Retrieved Params for repo [{}] as [{}]", repoUrl, repo);
    return repo;
  }
}
