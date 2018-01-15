package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.*;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.LIST_IDENTIFIERS_VALUE;
import static eu.cessda.pasc.osmhhandler.oaipmh.helpers.OaiPmhConstants.RESUMPTION_TOKEN_KEY;

/**
 * Helper methods to deal with Oai-pmh protocol
 *
 * @author moses@doraventures.com
 */
public class OaiPmhHelpers {

  public static String appendListRecordParams(String repoUrl) {
    return String.format(LIST_RECORD_HEADERS_URL_TEMPLATE, repoUrl,
        VERB_PARAM_KEY, LIST_IDENTIFIERS_VALUE, METADATA_PREFIX_PARAM_KEY, METADATA_DDI_2_5_VALUE);
  }

  public static String appendListRecordResumptionToken(String baseRepoUrl, String resumptionToken) {
    return String.format(LIST_RECORD_HEADERS_RESUMPTION_URL_TEMPLATE, baseRepoUrl,
        VERB_PARAM_KEY, LIST_IDENTIFIERS_VALUE, RESUMPTION_TOKEN_KEY, resumptionToken);
  }
}
