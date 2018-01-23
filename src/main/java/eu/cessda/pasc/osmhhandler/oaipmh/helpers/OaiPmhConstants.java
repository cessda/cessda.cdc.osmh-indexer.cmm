package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import org.jdom2.Namespace;

/**
 * OaiPmh related attributes and element Constants
 *
 * @author moses@doraventures.com
 */
public class OaiPmhConstants {

  public static final String LIST_RECORD_HEADERS_URL_TEMPLATE = "%s?%s=%s&%s=%s";
  public static final String LIST_RECORD_HEADERS_RESUMPTION_URL_TEMPLATE = "%s?%s=%s&%s=%s";
  public static final String COMPLETE_LIST_SIZE = "completeListSize";

  public static final String OAI_NS_PATH = "http://www.openarchives.org/OAI/2.0/";
  public static final String DDI_NS_PATH = "ddi:codebook:2_5";

  public static final Namespace XML_NS = Namespace.getNamespace("xml", "http://www.w3.org/XML/1998/namespace");
  public static final Namespace OAI_NS = Namespace.getNamespace("oai", OaiPmhConstants.OAI_NS_PATH);
  public static final Namespace DDI_NS = Namespace.getNamespace("ddi", DDI_NS_PATH);
  public static final Namespace[] OAI_AND_DDI_NS = {OAI_NS, DDI_NS};
  public static final Namespace[] DDI_AND_XML_NS = {DDI_NS, XML_NS};

  public static final String STUDY_CITATION_XPATH = "//ddi:stdyDscr/ddi:citation";
  public static final String IDENTIFIER_XPATH = "//oai:header/oai:identifier[1]";
  public static final String RECORD_STATUS_XPATH = "//oai:header/@status";
  public static final String LAST_MODIFIED_DATE_XPATH = "//oai:header/datestamp[1]";
  public static final String YEAR_OF_PUB_XPATH = "//ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:distDate[1]";
  public static final String ABSTRACT_XPATH = "//ddi:stdyInfo/ddi:abstract";

  public static final String PID_STUDY_XPATH = "//ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:IDNo";
  public static final String PERSON_NAME_XPATH = "//ddi:stdyDscr/ddi:citation/ddi:rspStmt/ddi:AuthEnty[1]";
  public static final String ACCESS_CLASS_XPATH = "//ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:restrctn[1]";
  public static final String DATA_ACCESS_XPATH = "//ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:conditions[1]";
  public static final String DATA_COLLECTION_PERIODS_PATH = "//ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:collDate[position()<3]";
  public static final String CLASSIFICATIONS_XPATH = "//ddi:subject/ddi:topcClas";
  public static final String KEYWORDS_XPATH = "//ddi:subject/ddi:keyword";
  public static final String TYPE_OF_TIME_METHOD_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:timeMeth";
  public static final String STUDY_AREA_COUNTRIES_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:nation";
  public static final String UNIT_TYPE_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:anlyUnit";
  public static final String PUBLISHER_XPATH = "//ddi:codeBook/ddi:docDscr/ddi:citation/ddi:prodStmt/ddi:producer";
  public static final String FILE_LANGUAGES_XPATH = "//ddi:codeBook/ddi:fileDscr/ddi:fileTxt/@xml:lang";
  public static final String TYPE_OF_SAMPLING_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:sampProc";
  public static final String SAMPLING_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:sampProc";
  public static final String TYPE_OF_MODE_OF_COLLECTION_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:collMode";
  public static final String INST_FULL_NAME_XPATH = "//ddi:stdyDscr/ddi:citation/ddi:rspStmt/ddi:AuthEnty";

  // Elements
  public static final String TITLE_STMT = "titlStmt";
  public static final String LANG_ATTR = "lang";
  public static final String TITLE = "titl";
  public static final String IDENTIFIER_ELEMENT = "identifier";
  public static final String DATESTAMP_ELEMENT = "datestamp";
  public static final String SET_SPEC_ELEMENT = "setSpec";
  public static final String HEADER_ELEMENT = "header";
  public static final String RESUMPTION_TOKEN_ELEMENT = "resumptionToken";
  public static final String DATE_ATTR = "date";
  public static final String END_ATTR = "end";
  public static final String START_ATTR = "start";
  public static final String SINGLE_ATTR = "single";

  // Encoding
  public static final String UTF_8 = "UTF-8";

  // URL Paths tokens
  public static final String VERB_PARAM_KEY = "verb";
  public static final String METADATA_PREFIX_PARAM_KEY = "metadataPrefix";
  public static final String RESUMPTION_TOKEN_KEY = RESUMPTION_TOKEN_ELEMENT;
  public static final String LIST_IDENTIFIERS_VALUE = "ListIdentifiers";
  public static final String METADATA_DDI_2_5_VALUE = "ddi";

  public static final String EVENT_ATTR = "event";

  private OaiPmhConstants() {
    // Hides implicit public constructor | For static constants only
  }
}
