/*
 * Copyright © 2017-2021 CESSDA ERIC (support@cessda.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.cessda.pasc.oci.parser;

import lombok.experimental.UtilityClass;
import org.jdom2.Namespace;

/**
 * OaiPmh related attributes and element Constants
 *
 * @author moses AT doraventures DOT com
 */
@UtilityClass
public class OaiPmhConstants {
    // Namespaces
    static final Namespace OAI_NS = Namespace.getNamespace("oai", "http://www.openarchives.org/OAI/2.0/");
    static final Namespace DDI_NS = Namespace.getNamespace("ddi", "ddi:codebook:2_5");
    static final Namespace[] OAI_AND_DDI_NS = {OAI_NS, DDI_NS};

    // General Paths
    static final String IDENTIFIER_XPATH = "//oai:header/oai:identifier[1]";
    static final String RECORD_STATUS_XPATH = "//oai:header/@status";
    static final String LAST_MODIFIED_DATE_XPATH = "//oai:header/oai:datestamp[1]";
    static final String ERROR_PATH = "//oai:error";

    // Elements
    static final String IDENTIFIER_ELEMENT = "identifier";
    static final String DATESTAMP_ELEMENT = "datestamp";
    static final String SET_SPEC_ELEMENT = "setSpec";
    static final String HEADER_ELEMENT = "header";
    static final String RESUMPTION_TOKEN_ELEMENT = "resumptionToken";
    static final String DELETED = "deleted";

    // Codebook Paths
    static final String ABSTRACT_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:abstract";
    static final String TITLE_XPATH = "//ddi:codeBook//ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:titl";
    static final String PAR_TITLE_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:parTitl";
    static final String STUDY_URL_DOC_DSCR_XPATH = "//ddi:codeBook/ddi:docDscr/ddi:citation/ddi:holdings";
    static final String STUDY_URL_STDY_DSCR_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:holdings";
    static final String PID_STUDY_XPATH = "//ddi:codeBook//ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:IDNo";
    static final String CREATORS_XPATH = "//ddi:codeBook//ddi:stdyDscr/ddi:citation/ddi:rspStmt/ddi:AuthEnty";
    static final String DATA_RESTRCTN_XPATH = "//ddi:codeBook//ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:restrctn";
    static final String DATA_COLLECTION_PERIODS_PATH = "//ddi:codeBook//ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:collDate";
    static final String YEAR_OF_PUB_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:distDate[1]/@date";
    static final String CLASSIFICATIONS_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:subject/ddi:topcClas";
    static final String KEYWORDS_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:subject/ddi:keyword";
    static final String TYPE_OF_TIME_METHOD_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:timeMeth";
    static final String STUDY_AREA_COUNTRIES_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:nation";
    static final String UNIT_TYPE_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:anlyUnit";
    static final String PUBLISHER_XPATH = "//ddi:codeBook/ddi:docDscr/ddi:citation/ddi:prodStmt/ddi:producer";
    static final String DISTRIBUTOR_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:distrbtr";
    static final String FILE_TXT_LANGUAGES_XPATH = "//ddi:codeBook/ddi:fileDscr/ddi:fileTxt/@xml:lang";
    static final String FILENAME_LANGUAGES_XPATH = "//ddi:codeBook/ddi:fileDscr/ddi:fileTxt/ddi:fileName/@xml:lang";
    static final String TYPE_OF_SAMPLING_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:sampProc";
    static final String TYPE_OF_MODE_OF_COLLECTION_XPATH = "//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:collMode";
    static final String RECORD_DEFAULT_LANGUAGE_XPATH = "//ddi:codeBook/@xml:lang";

    // Attributes
    public static final String STATUS_ATTR = "status";
    static final String LANG_ATTR = "lang";
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
    static final String URI_ATTR = "URI";
    static final String OAI_PMH = "OAI-PMH";
    static final String ERROR = "error";

    // URL Paths tokens
    static final String VERB_PARAM_KEY = "verb";
    static final String METADATA_PREFIX_PARAM_KEY = "metadataPrefix";
    static final String SET_SPEC_PARAM_KEY = "set";
    static final String IDENTIFIER_PARAM_KEY = IDENTIFIER_ELEMENT;
    static final String RESUMPTION_TOKEN_KEY = RESUMPTION_TOKEN_ELEMENT;
    static final String LIST_IDENTIFIERS_VALUE = "ListIdentifiers";
    static final String GET_RECORD_VALUE = "GetRecord";
}
