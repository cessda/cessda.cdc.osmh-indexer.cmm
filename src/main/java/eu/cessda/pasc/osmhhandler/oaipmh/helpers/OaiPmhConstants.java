package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

/**
 * OaiPmh related attributes and element Constants
 *
 * @author moses@doraventures.com
 */
public class OaiPmhConstants {

  public static final String LIST_RECORD_HEADERS_URL_TEMPLATE = "%s?%s=%s&%s=%s";
  public static final String LIST_RECORD_HEADERS_RESUMPTION_URL_TEMPLATE = "%s?%s=%s&%s=%s";
  public static final String COMPLETE_LIST_SIZE = "completeListSize";

  private OaiPmhConstants() {
    // Hides implicit public constructor | For static constants only
  }

  // Elements
  public static final String IDENTIFIER_ELEMENT = "identifier";
  public static final String DATESTAMP_ELEMENT = "datestamp";
  public static final String SET_SPEC_ELEMENT = "setSpec";
  public static final String HEADER_ELEMENT = "header";
  public static final String RESUMPTION_TOKEN_ELEMENT = "resumptionToken";

  // Encoding
  public static final String UTF_8 = "UTF-8";

  // Paths
  public static final String VERB_PARAM_KEY = "verb";
  public static final String METADATA_PREFIX_PARAM_KEY = "metadataPrefix";
  public static final String RESUMPTION_TOKEN_KEY = "resumptionToken";
  public static final String LIST_IDENTIFIERS_VALUE = "ListIdentifiers";
  public static final String METADATA_DDI_2_5_VALUE="ddi";
}
