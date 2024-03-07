/*
 * Copyright © 2017-2024 CESSDA ERIC (support@cessda.eu)
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

    // Elements
    static final String IDENTIFIER = "identifier";
    static final String DATESTAMP_ELEMENT = "datestamp";
    static final String RECORD_ELEMENT = "//oai:record";
    static final String DELETED = "deleted";

    // Attributes
    static final String STATUS_ATTR = "status";
    static final String LANG_ATTR = "lang";
    static final String DATE_ATTR = "date";
    static final String END_ATTR = "end";
    static final String START_ATTR = "start";
    static final String SINGLE_ATTR = "single";
    static final String EVENT_ATTR = "event";
    static final String CREATOR_AFFILIATION_ATTR = "affiliation";
    static final String VOCAB_ATTR = "vocab";
    static final String VOCAB_URI_ATTR = "vocabURI";
    static final String ID_ATTR = "ID";
    static final String ABBR_ATTR = "abbr";
    static final String AGENCY_ATTR = "agency";
    static final String CONCEPT_EL = "concept";
    static final String URI_ATTR = "URI";

}
