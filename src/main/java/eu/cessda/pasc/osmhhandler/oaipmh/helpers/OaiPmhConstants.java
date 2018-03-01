package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import org.jdom2.Namespace;

/**
 * OaiPmh related attributes and element Constants
 *
 * @author moses@doraventures.com
 */
public class OaiPmhConstants {

  private static final String OAI_NS_PATH = "http://www.openarchives.org/OAI/2.0/";
  private static final String DDI_NS_PATH = "ddi:codebook:2_5";

  // Name Spaces
  static final Namespace XML_NS = Namespace.getNamespace("xml", "http://www.w3.org/XML/1998/namespace");
  static final Namespace OAI_NS = Namespace.getNamespace("oai", OaiPmhConstants.OAI_NS_PATH);
  static final Namespace DDI_NS = Namespace.getNamespace("ddi", DDI_NS_PATH);
  static final Namespace[] OAI_AND_DDI_NS = {OAI_NS, DDI_NS};

  // General Paths
  static final String IDENTIFIER_XPATH = "//oai:header/oai:identifier[1]";
  static final String RECORD_STATUS_XPATH = "//oai:header/@status";
  static final String LAST_MODIFIED_DATE_XPATH = "//oai:header/oai:datestamp[1]";
  static final String ERROR_PATH = "//oai:error";

  // Codebook Paths
  static final String YEAR_OF_PUB_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:distDate[1]/@date";
  static final String ABSTRACT_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:abstract";
  static final String TITLE_XPATH = "//ddi:codeBook//ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:titl";
  static final String PID_STUDY_XPATH = "//ddi:codeBook//ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:IDNo"; // TODO use @agency instead
  static final String CREATORS_XPATH = "//ddi:codeBook//ddi:stdyDscr/ddi:citation/ddi:rspStmt/ddi:AuthEnty";
  static final String DATA_RESTRCTN_XPATH = "//ddi:codeBook//ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:restrctn";
  static final String DATA_CONDITIONS_XPATH = "//ddi:codeBook//ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:conditions";
  static final String DATA_SPEC_PERM_XPATH = "//ddi:codeBook//ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:specPerm";
  static final String DATA_AVL_STATUS_XPATH = "//ddi:codeBook//ddi:stdyDscr/ddi:dataAccs/ddi:setAvail/ddi:avlStatus";
  static final String DATA_COLLECTION_PERIODS_PATH = "//ddi:codeBook//ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:collDate";
  static final String CLASSIFICATIONS_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:subject/ddi:topcClas";

  static final String KEYWORDS_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:subject/ddi:keyword";
  static final String TYPE_OF_TIME_METHOD_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:timeMeth";
  static final String STUDY_AREA_COUNTRIES_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:nation";
  static final String UNIT_TYPE_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:anlyUnit";

  static final String PUBLISHER_XPATH = "//ddi:codeBook/ddi:docDscr/ddi:citation/ddi:prodStmt/ddi:producer";
  static final String FILE_TXT_LANGUAGES_XPATH = "//ddi:codeBook/ddi:fileDscr/ddi:fileTxt/@xml:lang";
  static final String FILENAME_LANGUAGES_XPATH = "//ddi:codeBook/ddi:fileDscr/ddi:fileTxt/ddi:fileName/@xml:lang";
  static final String TYPE_OF_SAMPLING_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:sampProc";
  static final String SAMPLING_XPATH = TYPE_OF_SAMPLING_XPATH;
  static final String TYPE_OF_MODE_OF_COLLECTION_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:collMode";

  // Elements
  static final String LANG_ATTR = "lang";
  public static final String IDENTIFIER_ELEMENT = "identifier";
  public static final String DATESTAMP_ELEMENT = "datestamp";
  public static final String SET_SPEC_ELEMENT = "setSpec";
  public static final String HEADER_ELEMENT = "header";
  public static final String RESUMPTION_TOKEN_ELEMENT = "resumptionToken";

  // Attributes
  static final String DATE_ATTR = "date";
  static final String END_ATTR = "end";
  static final String START_ATTR = "start";
  static final String SINGLE_ATTR = "single";
  static final String EVENT_ATTR = "event";
  static final String CODE_ATTR = "code";
  static final String CREATOR_AFFILIATION_ATTR = "affiliation";
  static final String VOCAB_ATTR = "vocab";
  static final String VOCAB_URI_ATTR = "vocabURI";
  static final String ID_ATTR = "ID";
  static final String ABBR_ATTR = "abbr";
  static final String AGENCY_ATTR = "agency";
  static final String CONCEPT_EL = "concept";
  public static final String COMPLETE_LIST_SIZE_ATTR = "completeListSize";
  public static final String OAI_PMH = "OAI-PMH";
  public static final String ERROR = "error";

  // Encoding
  public static final String UTF_8 = "UTF-8";

  // URL Paths tokens
  static final String VERB_PARAM_KEY = "verb";
  static final String METADATA_PREFIX_PARAM_KEY = "metadataPrefix";
  static final String IDENTIFIER_PARAM_KEY = IDENTIFIER_ELEMENT;
  static final String RESUMPTION_TOKEN_KEY = RESUMPTION_TOKEN_ELEMENT;
  static final String LIST_IDENTIFIERS_VALUE = "ListIdentifiers";
  static final String GET_RECORD_VALUE = "GetRecord";
  static final String METADATA_DDI_2_5_VALUE = "ddi";

  static final String LIST_RECORD_HEADERS_URL_TEMPLATE = "%s?%s=%s&%s=%s";
  static final String GET_RECORD_URL_TEMPLATE = "%s?%s=%s&%s=%s&%s=%s";

  private OaiPmhConstants() {
    throw new UnsupportedOperationException("Utility class, instantiation not allow");
  }
}
