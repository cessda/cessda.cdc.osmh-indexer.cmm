package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.OaiPmh;
import eu.cessda.pasc.osmhhandler.oaipmh.models.configuration.Repo;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.*;

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

  public static String appendListRecordParams(String repoUrl, OaiPmh oaiPmh) {
    String metadata = getMetadataPrefix(repoUrl, oaiPmh);
    return String.format(LIST_RECORD_HEADERS_URL_TEMPLATE, repoUrl,
        VERB_PARAM_KEY, LIST_IDENTIFIERS_VALUE, // verb=ListIdentifier
        METADATA_PREFIX_PARAM_KEY, metadata //&metadataPrefix=ddi
    );
  }

  public static String appendListRecordResumptionToken(String baseRepoUrl, String resumptionToken) {
    return String.format(LIST_RECORD_HEADERS_URL_TEMPLATE, baseRepoUrl,
        VERB_PARAM_KEY, LIST_IDENTIFIERS_VALUE, // verb=ListIdentifier
        RESUMPTION_TOKEN_KEY, resumptionToken // &resumptionToken=0001/500....
    );
  }

  public static String appendGetRecordParams(String repoUrl, String identifier, OaiPmh oaiPmh) {
    String metadata = getMetadataPrefix(repoUrl, oaiPmh);
    return String.format(GET_RECORD_URL_TEMPLATE, repoUrl,
        VERB_PARAM_KEY, GET_RECORD_VALUE, // verb=GetRecord
        IDENTIFIER_PARAM_KEY, identifier, //&identifier=1683
        METADATA_PREFIX_PARAM_KEY, metadata //&metadataPrefix=ddi
    );
  }

  public static String decodeStudyNumber(String encodedStudyNumber){
    return encodedStudyNumber.replaceAll("_dt_", ".")
        .replaceAll("_sl_", "/")
        .replaceAll("_cl_", ":");
  }

  private static String getMetadataPrefix(String repoUrl, OaiPmh oaiPmh) {
    Optional<Repo> optRepo = oaiPmh.getRepos().stream()
        .filter(repo -> repo.getUrl().equalsIgnoreCase(repoUrl)).findFirst();
    String metadataPrefix = optRepo.map(Repo::getPreferredMetadataParam).orElse(METADATA_DDI_2_5_VALUE);
    log.info("Configured Metadata format for [{}] as [{}]", repoUrl, metadataPrefix);
    return metadataPrefix;
  }
}
