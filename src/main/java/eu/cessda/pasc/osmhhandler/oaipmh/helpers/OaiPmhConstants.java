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
  public static final String STUDY_CITATION_XPATH = "//ddi:stdyDscr/ddi:citation";
  public static final String IDENTIFIER_XPATH = "//oai:header/oai:identifier";
  public static final String TITLE_STMT = "titlStmt";
  public static final String TITLE = "titl";
  public static final String UNKNOWN_LANG = "xx";
  public static final String LANG = "lang";

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
  public static final String METADATA_DDI_2_5_VALUE = "ddi";
}
