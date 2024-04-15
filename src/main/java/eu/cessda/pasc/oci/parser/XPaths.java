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

import eu.cessda.pasc.oci.DateNotParsedException;
import eu.cessda.pasc.oci.exception.UnsupportedXMLNamespaceException;
import eu.cessda.pasc.oci.models.cmmstudy.*;
import lombok.*;
import org.jdom2.Element;
import org.jdom2.Namespace;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

import static eu.cessda.pasc.oci.parser.ParsingStrategies.*;
import static eu.cessda.pasc.oci.parser.XMLMapper.*;
import static java.util.Map.entry;

/**
 * XPath constants used to extract metadata from DDI XML documents.
 */
@Builder
@EqualsAndHashCode
@Getter(value = AccessLevel.PACKAGE)
@ToString
@With
@SuppressWarnings("UnnecessaryLambda")
public final class XPaths {

    @NonNull
    private final Namespace[] namespace;
    // Codebook Paths
    private final String recordDefaultLanguage;
    private final XMLMapper<Optional<String>> yearOfPubXPath;
    private final XMLMapper<Map<String, String>> abstractXPath;
    private final XMLMapper<Map<String, String>> titleXPath;
    @Nullable
    private final XMLMapper<Map<String, String>> parTitleXPath;
    @Nullable
    private final XMLMapper<Map<String, List<String>>> dataAccessUrlXPath;
    @Nullable
    private final XMLMapper<Map<String, List<String>>> studyURLDocDscrXPath;
    private final XMLMapper<Map<String, List<String>>> studyURLStudyDscrXPath;
    private final XMLMapper<Map<String, List<Pid>>> pidStudyXPath;
    private final XMLMapper<Map<String, List<String>>> dataRestrctnXPath;
    private final XMLMapper<CMMStudyMapper.ParseResults<CMMStudyMapper.DataCollectionPeriod, List<DateNotParsedException>>> dataCollectionPeriodsXPath;
    private final XMLMapper<Map<String, List<TermVocabAttributes>>> classificationsXPath;
    private final XMLMapper<Map<String, List<TermVocabAttributes>>> keywordsXPath;
    private final XMLMapper<Map<String, List<TermVocabAttributes>>> typeOfTimeMethodXPath;
    private final XMLMapper<Map<String, List<Country>>> studyAreaCountriesXPath;
    private final XMLMapper<Map<String, List<TermVocabAttributes>>> unitTypeXPath;
    private final XMLMapper<Map<String, Publisher>> publisherXPath;
    @Nullable
    private final XMLMapper<Map<String, Publisher>> distributorXPath;
    @Nullable
    private final XMLMapper<Set<String>> fileTxtLanguagesXPath;
    @Nullable
    private final XMLMapper<Set<String>> filenameLanguagesXPath;
    private final XMLMapper<Map<String, List<TermVocabAttributes>>> samplingXPath;
    private final XMLMapper<Map<String, List<TermVocabAttributes>>> typeOfModeOfCollectionXPath;
    private final XMLMapper<Map<String, List<RelatedPublication>>> relatedPublicationsXPath;
    @Nullable
    private final XMLMapper<Map<String, List<UniverseElement>>> universeXPath;
    private final XMLMapper<Map<String, List<String>>> creatorsXPath;
    private final XMLMapper<Map<String, List<Funding>>> fundingXPath;

    private static final CMMStudyMapper.ParseResults<CMMStudyMapper.DataCollectionPeriod, List<DateNotParsedException>> EMPTY_PARSE_RESULTS = new CMMStudyMapper.ParseResults<>(
        new CMMStudyMapper.DataCollectionPeriod(null, 0, null, Collections.emptyMap()),
        Collections.emptyList()
    );

    private static final TermVocabAttributeNames DDI_3_2_ATTR_NAMES = new TermVocabAttributeNames("codeListName", "codeListURN");
    private static final Function<Element, Optional<TermVocabAttributes>> TERM_VOCAB_ATTR_3_2_STRATEGY = (Element element) ->
        ParsingStrategies.termVocabAttributeLifecycleStrategy(element, DDI_3_2_ATTR_NAMES);

    /**
     * XPaths needed to extract metadata from DDI 3.2 documents.
     */
    public static final XPaths DDI_3_2_XPATHS = XPaths.builder()
        .namespace(new Namespace[]{
            Namespace.getNamespace("ddi", "ddi:instance:3_2"),
            Namespace.getNamespace("a", "ddi:archive:3_2"),
            Namespace.getNamespace("c", "ddi:conceptualcomponent:3_2"),
            Namespace.getNamespace("d","ddi:datacollection:3_2"),
            Namespace.getNamespace("g", "ddi:group:3_2"),
            Namespace.getNamespace("pi", "ddi:physicalinstance:3_2"),
            Namespace.getNamespace("r", "ddi:reusable:3_2"),
            Namespace.getNamespace("s", "ddi:studyunit:3_2")
        })
        // Abstract
        .abstractXPath(new XMLMapper<>("//s:StudyUnit[1]/r:Abstract/r:Content", parseLanguageContentOfElement(Element::getTextTrim)))
        // Study title
        .titleXPath(new XMLMapper<>("//s:StudyUnit[1]/r:Citation/r:Title/r:String", parseLanguageContentOfElement(Element::getTextTrim)))
        // 'Access study' link (when @typeOfUserID attribute is "URLServiceProvider")
        .studyURLDocDscrXPath(new XMLMapper<>("//s:StudyUnit[1]/r:UserID", extractMetadataObjectListForEachLang(ParsingStrategies::uriStrategy)))
        // URL of the study description page at the SP website (when @typeOfUserID attribute is "URLServiceProvider")
        .studyURLStudyDscrXPath(new XMLMapper<>("//s:StudyUnit[1]/r:UserID", extractMetadataObjectListForEachLang(ParsingStrategies::uriStrategy)))
        // Study number/PID
        .pidStudyXPath(new XMLMapper<>("//s:StudyUnit[1]/r:Citation/r:InternationalIdentifier", extractMetadataObjectListForEachLang(ParsingStrategies::pidLifecycleStrategy)))
        // Creator/PI
        .creatorsXPath(new XMLMapper<>("//s:StudyUnit[1]/r:Citation/r:Creator", ParsingStrategies::creatorsStrategy))
        // Terms of data access
        .dataRestrctnXPath(new XMLMapper<>("//s:StudyUnit[1]/a:Archive/a:ArchiveSpecific/a:Item/a:Access/r:Description/r:Content", extractMetadataObjectListForEachLang(ParsingStrategies::nullableElementValueStrategy)))
        // Data collection period
        .dataCollectionPeriodsXPath(new XMLMapper<>("//s:StudyUnit[1]/d:DataCollection/d:CollectionEvent/d:DataCollectionDate", elementList -> getFirstEntry(ParsingStrategies::dataCollectionPeriodsLifecycleStrategy).apply(elementList).orElse(EMPTY_PARSE_RESULTS)))
        // Publication year
        .yearOfPubXPath(new XMLMapper<>("//s:StudyUnit[1]/r:Citation/r:PublicationDate/r:SimpleDate", getFirstEntry(Element::getTextTrim)))
        // Topics
        .classificationsXPath(new XMLMapper<>("//s:StudyUnit[1]/r:Coverage/r:TopicalCoverage/r:Subject", extractMetadataObjectListForEachLang(TERM_VOCAB_ATTR_3_2_STRATEGY)))
        // Keywords
        .keywordsXPath(new XMLMapper<>("//s:StudyUnit[1]/r:Coverage/r:TopicalCoverage/r:Keyword", extractMetadataObjectListForEachLang(TERM_VOCAB_ATTR_3_2_STRATEGY)))
        // Time dimension
        .typeOfTimeMethodXPath(new XMLMapper<>("//s:StudyUnit[1]/d:DataCollection/d:Methodology/d:TimeMethod", (List<Element> elementList) -> typeOfTimeMethodLifecycleStrategy(elementList, DDI_3_2_ATTR_NAMES)))
        // Country
        .studyAreaCountriesXPath(new XMLMapper<>("//s:StudyUnit[1]/r:Coverage/r:SpatialCoverage/r:GeographicLocationReference", ParsingStrategies::geographicLocationStrategy))
        // Analysis unit
        .unitTypeXPath(new XMLMapper<>("//s:StudyUnit[1]/r:AnalysisUnit", (List<Element> elementList) -> analysisUnitStrategy(elementList, DDI_3_2_ATTR_NAMES)))
        // Publisher
        .publisherXPath(new XMLMapper<>("//s:StudyUnit[1]/r:Citation/r:Publisher/r:PublisherReference", ParsingStrategies::publisherReferenceStrategy))
        // Language of data file(s)
        .fileTxtLanguagesXPath(new XMLMapper<>( "//ddi:DDIInstance/g:ResourcePackage/pi:PhysicalInstance/r:Citation/r:Language", XMLMapper::getLanguageFromElements))
        // Language-specific name of file
        .filenameLanguagesXPath(new XMLMapper<>("//ddi:DDIInstance/g:ResourcePackage/pi:PhysicalInstance/r:Citation/r:Title/r:String", XMLMapper::getLanguagesOfElements))
        // Sampling procedure
        .samplingXPath(new XMLMapper<>("//s:StudyUnit[1]/d:DataCollection/d:Methodology/d:SamplingProcedure", (List<Element> elementList) -> samplingProceduresLifecycleStrategy(elementList, DDI_3_2_ATTR_NAMES)))
        // Data collection mode
        .typeOfModeOfCollectionXPath(new XMLMapper<>("//s:StudyUnit[1]/d:DataCollection/d:CollectionEvent/d:ModeOfCollection", (List<Element> elementList) -> typeOfModeOfCollectionLifecycleStrategy(elementList, DDI_3_2_ATTR_NAMES)))
        // PID of Related publication
        .relatedPublicationsXPath(new XMLMapper<>("//s:StudyUnit[1]/r:OtherMaterial", ParsingStrategies::relatedPublicationLifecycleStrategy))
        /// /r:Citation/r:InternationalIdentifier/r:IdentifierContent
        // Study description language
        .recordDefaultLanguage("//ddi:DDIInstance/@xml:lang")
        // Description of population
        .universeXPath(new XMLMapper<>("//s:StudyUnit[1]/c:ConceptualComponent/c:UniverseScheme/c:Universe", ParsingStrategies::universeLifecycleStrategy))
        // Funding information
        .fundingXPath(new XMLMapper<>("//s:StudyUnit[1]/r:FundingInformation", ParsingStrategies::fundingLifecycleStrategy))
        .build();

    private static final TermVocabAttributeNames DDI_3_3_ATTR_NAMES = new TermVocabAttributeNames("controlledVocabularyName", "controlledVocabularyURN");
    private static final Function<Element, Optional<TermVocabAttributes>> TERM_VOCAB_ATTR_3_3_STRATEGY = (Element element) ->
        ParsingStrategies.termVocabAttributeLifecycleStrategy(element, DDI_3_3_ATTR_NAMES);

    /**
     * XPaths needed to extract metadata from DDI 3.3 documents.
     */
    public static final XPaths DDI_3_3_XPATHS = DDI_3_2_XPATHS
        .withNamespace(new Namespace[]{
            Namespace.getNamespace("ddi", "ddi:instance:3_3"),
            Namespace.getNamespace("a", "ddi:archive:3_3"),
            Namespace.getNamespace("c", "ddi:conceptualcomponent:3_3"),
            Namespace.getNamespace("d","ddi:datacollection:3_3"),
            Namespace.getNamespace("g", "ddi:group:3_3"),
            Namespace.getNamespace("pi", "ddi:physicalinstance:3_3"),
            Namespace.getNamespace("r", "ddi:reusable:3_3"),
            Namespace.getNamespace("s", "ddi:studyunit:3_3")
        })
        // PID of Related publication
        .withRelatedPublicationsXPath(new XMLMapper<>("//s:StudyUnit[1]/r:OtherMaterialScheme/r:OtherMaterial", ParsingStrategies::relatedPublicationLifecycleStrategy))
        // Topics
        .withClassificationsXPath(new XMLMapper<>("//s:StudyUnit[1]/r:Coverage/r:TopicalCoverage/r:Subject", extractMetadataObjectListForEachLang(TERM_VOCAB_ATTR_3_3_STRATEGY)))
        // Keywords
        .withKeywordsXPath(new XMLMapper<>("//s:StudyUnit[1]/r:Coverage/r:TopicalCoverage/r:Keyword", extractMetadataObjectListForEachLang(TERM_VOCAB_ATTR_3_3_STRATEGY)))
        // Time dimension
        .withTypeOfTimeMethodXPath(new XMLMapper<>("//s:StudyUnit[1]/d:DataCollection/d:Methodology/d:TimeMethod", (List<Element> elementList) -> ParsingStrategies.typeOfTimeMethodLifecycleStrategy(elementList, DDI_3_3_ATTR_NAMES)))
        // Analysis unit
        .withUnitTypeXPath(new XMLMapper<>("//s:StudyUnit[1]/r:AnalysisUnit", (List<Element> elementList) -> ParsingStrategies.analysisUnitStrategy(elementList, DDI_3_3_ATTR_NAMES)))
        // Sampling procedure
        .withSamplingXPath(new XMLMapper<>("//s:StudyUnit[1]/d:DataCollection/d:Methodology/d:SamplingProcedure", (List<Element> elementList) -> ParsingStrategies.samplingProceduresLifecycleStrategy(elementList, DDI_3_3_ATTR_NAMES)))
        // Data collection mode
        .withTypeOfModeOfCollectionXPath(new XMLMapper<>("//s:StudyUnit[1]/d:DataCollection/d:CollectionEvent/d:ModeOfCollection", (List<Element> elementList) -> ParsingStrategies.typeOfModeOfCollectionLifecycleStrategy(elementList, DDI_3_3_ATTR_NAMES)));


    Optional<XMLMapper<Map<String, String>>> getParTitleXPath() {
        return Optional.ofNullable(parTitleXPath);
    }

    Optional<XMLMapper<Map<String, List<String>>>> getDataAccessUrlXPath() {
        return Optional.ofNullable(dataAccessUrlXPath);
    }

    Optional<XMLMapper<Set<String>>> getFileTxtLanguagesXPath() {
        return Optional.ofNullable(fileTxtLanguagesXPath);
    }

    Optional<XMLMapper<Set<String>>> getFilenameLanguagesXPath() {
        return Optional.ofNullable(filenameLanguagesXPath);
    }

    Optional<XMLMapper<Map<String, List<String>>>> getStudyURLDocDscrXPath() {
        return Optional.ofNullable(studyURLDocDscrXPath);
    }

    Optional<XMLMapper<Map<String, List<UniverseElement>>>> getUniverseXPath() {
        return Optional.ofNullable(universeXPath);
    }
    /**
     * XPaths needed to extract metadata from DDI 2.5 documents.
     */
    public static final XPaths DDI_2_5_XPATHS = XPaths.builder()
        .namespace(new Namespace[]{ Namespace.getNamespace("ddi", "ddi:codebook:2_5") })
        // Abstract
        .abstractXPath(new XMLMapper<>("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:abstract", parseLanguageContentOfElement(Element::getTextTrim, (a, b) -> a + "<br>" + b)))
        // Study title
        .titleXPath(new XMLMapper<>("//ddi:codeBook//ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:titl", parseLanguageContentOfElement(Element::getTextTrim)))
        // Study title (in additional languages)
        .parTitleXPath(new XMLMapper<>("//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:parTitl", parseLanguageContentOfElement(Element::getTextTrim)))
        // 'Access study' link
        .studyURLDocDscrXPath(new XMLMapper<>("//ddi:codeBook/ddi:docDscr/ddi:citation/ddi:holdings", extractMetadataObjectListForEachLang(ParsingStrategies::uriStrategy)))
        // URL of the study description page at the SP website
        .studyURLStudyDscrXPath(new XMLMapper<>("//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:holdings", extractMetadataObjectListForEachLang(ParsingStrategies::uriStrategy)))
        // Study number/PID
        .pidStudyXPath(new XMLMapper<>("//ddi:codeBook//ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:IDNo", extractMetadataObjectListForEachLang(ParsingStrategies::pidStrategy)))
        // Creator
        .creatorsXPath(new XMLMapper<>("//ddi:codeBook//ddi:stdyDscr/ddi:citation/ddi:rspStmt/ddi:AuthEnty", extractMetadataObjectListForEachLang(ParsingStrategies::creatorStrategy)))
        // Terms of data access
        .dataAccessUrlXPath(new XMLMapper<>("//ddi:codeBook//ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:specPerm", extractMetadataObjectListForEachLang(ParsingStrategies::uriStrategy)))
        // Terms of data access
        .dataRestrctnXPath(new XMLMapper<>("//ddi:codeBook//ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:restrctn", extractMetadataObjectListForEachLang(ParsingStrategies::nullableElementValueStrategy)))
        // Data collection period
        .dataCollectionPeriodsXPath(new XMLMapper<>("//ddi:codeBook//ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:collDate", ParsingStrategies::dataCollectionPeriodsStrategy))
        // Publication year
        .yearOfPubXPath(new XMLMapper<>("//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:distDate[1]", getFirstEntry(ParsingStrategies::dateStrategy)))
        // Topics
        .classificationsXPath(new XMLMapper<>("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:subject/ddi:topcClas", extractMetadataObjectListForEachLang(element -> ParsingStrategies.termVocabAttributeStrategy(element, false))))
        // Keywords
        .keywordsXPath(new XMLMapper<>("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:subject/ddi:keyword", extractMetadataObjectListForEachLang(element -> ParsingStrategies.termVocabAttributeStrategy(element, false))))
        // Time dimension
        .typeOfTimeMethodXPath(new XMLMapper<>("//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:timeMeth", elementList -> ParsingStrategies.conceptStrategy(elementList, e -> ParsingStrategies.termVocabAttributeStrategy(e, true))))
        // Country
        .studyAreaCountriesXPath(new XMLMapper<>("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:nation", extractMetadataObjectListForEachLang(ParsingStrategies::countryStrategy)))
        // Analysis unit
        .unitTypeXPath(new XMLMapper<>("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:anlyUnit", elementList -> ParsingStrategies.conceptStrategy(elementList, ParsingStrategies::samplingTermVocabAttributeStrategy)))
        // Publisher name/Contributor
        .publisherXPath(new XMLMapper<>("//ddi:codeBook/ddi:docDscr/ddi:citation/ddi:prodStmt/ddi:producer", parseLanguageContentOfElement(ParsingStrategies::publisherStrategy)))
        // Publisher
        .distributorXPath(new XMLMapper<>("//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:distrbtr", parseLanguageContentOfElement(ParsingStrategies::publisherStrategy)))
        // Language of data file(s)
        .fileTxtLanguagesXPath(new XMLMapper<>( "//ddi:codeBook/ddi:fileDscr/ddi:fileTxt", XMLMapper::getLanguagesOfElements))
        // Language-specific name of file
        .filenameLanguagesXPath(new XMLMapper<>("//ddi:codeBook/ddi:fileDscr/ddi:fileTxt/ddi:fileName", XMLMapper::getLanguagesOfElements))
        // Sampling procedure
        .samplingXPath(new XMLMapper<>("//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:sampProc", elementList -> ParsingStrategies.conceptStrategy(elementList, ParsingStrategies::samplingTermVocabAttributeStrategy)))
        // Data collection mode
        .typeOfModeOfCollectionXPath(new XMLMapper<>("//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:collMode", elementList -> ParsingStrategies.conceptStrategy(elementList, e -> ParsingStrategies.termVocabAttributeStrategy(e, true))))
        // Related publication
        .relatedPublicationsXPath(new XMLMapper<>("//ddi:codeBook/ddi:stdyDscr/ddi:othrStdyMat/ddi:relPubl", extractMetadataObjectListForEachLang(ParsingStrategies::relatedPublicationsStrategy)))
        // Study description language
        .recordDefaultLanguage("//ddi:codeBook/@xml:lang")
        // Description of population
        .universeXPath(new XMLMapper<>("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:universe", extractMetadataObjectListForEachLang(ParsingStrategies::universeStrategy)))
        // Funding information
        .fundingXPath(new XMLMapper<>("//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:prodStmt/ddi:grantNo", extractMetadataObjectListForEachLang(ParsingStrategies::fundingStrategy)))
        .build();

    /**
     * XPaths needed to extract metadata from NESSTAR flavoured DDI 1.2.2 documents.
     */
    public static final XPaths NESSTAR_XPATHS = XPaths.builder()
        .namespace(new Namespace[]{ Namespace.getNamespace("ddi", "http://www.icpsr.umich.edu/DDI") })
        .recordDefaultLanguage("//ddi:codeBook/@xml-lang") // Nesstar with "-"
        // Closest for Nesstar based on CMM mapping doc but the above existing one for ddi2.5 seems to be present in Nesstar
        .yearOfPubXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/citation/distStmt/distDate[1]", getFirstEntry(ParsingStrategies::dateStrategy)))
        .abstractXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/stdyInfo/abstract", parseLanguageContentOfElement(Element::getTextTrim, (a, b) -> a + "<br>" + b)))
        .titleXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/citation/titlStmt/titl", parseLanguageContentOfElement(Element::getTextTrim)))
        .parTitleXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/citation/titlStmt/parTitl", parseLanguageContentOfElement(Element::getTextTrim)))
        .studyURLStudyDscrXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/dataAccs/setAvail/accsPlac", extractMetadataObjectListForEachLang(ParsingStrategies::uriStrategy)))
        // PID path missing for most nesstar repos. Available in FORS but:
        //  -No agency
        //  -Only element freetext value.
        .pidStudyXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/citation/titlStmt/IDNo", extractMetadataObjectListForEachLang(ParsingStrategies::pidStrategy))) // use @agency instead?
        .creatorsXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/citation/rspStmt/AuthEnty", extractMetadataObjectListForEachLang(ParsingStrategies::creatorStrategy)))
        .dataRestrctnXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/dataAccs/useStmt/restrctn", extractMetadataObjectListForEachLang(ParsingStrategies::nullableElementValueStrategy)))
        .dataCollectionPeriodsXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/stdyInfo/sumDscr/collDate", ParsingStrategies::dataCollectionPeriodsStrategy))
        .classificationsXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/stdyInfo/subject/topcClas", extractMetadataObjectListForEachLang(element -> ParsingStrategies.termVocabAttributeStrategy(element, false))))
        .keywordsXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/stdyInfo/subject/keyword", extractMetadataObjectListForEachLang(element -> ParsingStrategies.termVocabAttributeStrategy(element, false))))
        .typeOfTimeMethodXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/method/dataColl/timeMeth", elementList -> ParsingStrategies.conceptStrategy(elementList, e -> ParsingStrategies.termVocabAttributeStrategy(e, true))))
        .studyAreaCountriesXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/stdyInfo/sumDscr/nation", extractMetadataObjectListForEachLang(ParsingStrategies::countryStrategy)))
        .unitTypeXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/stdyInfo/sumDscr/anlyUnit", extractMetadataObjectListForEachLang(element -> termVocabAttributeStrategy(element, true))))
        .publisherXPath(new XMLMapper<>("//ddi:codeBook/docDscr/citation/prodStmt/producer", parseLanguageContentOfElement(ParsingStrategies::publisherStrategy)))
        .distributorXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/citation/distStmt/distrbtr", parseLanguageContentOfElement(ParsingStrategies::publisherStrategy)))
        .samplingXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/method/dataColl/sampProc", elementList -> ParsingStrategies.conceptStrategy(elementList, ParsingStrategies::samplingTermVocabAttributeStrategy)))
        .typeOfModeOfCollectionXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/method/dataColl/collMode",  elementList -> ParsingStrategies.conceptStrategy(elementList, e -> ParsingStrategies.termVocabAttributeStrategy(e, true))))
        .relatedPublicationsXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/othrStdyMat/relPubl", extractMetadataObjectListForEachLang(ParsingStrategies::relatedPublicationsStrategy)))
        .universeXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/stdyInfo/sumDscr/universe", extractMetadataObjectListForEachLang(ParsingStrategies::universeStrategy)))
        // Funding information
        .fundingXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/citation/prodStmt/grantNo", extractMetadataObjectListForEachLang(ParsingStrategies::fundingStrategy)))
        .build();

    /**
     * Mapping of XML namespaces to XPaths
     */
    private static final Map<Namespace, XPaths> XPATH_MAP = Map.ofEntries(
        entry(DDI_3_3_XPATHS.getNamespace()[0], DDI_3_3_XPATHS),
        entry(DDI_3_2_XPATHS.getNamespace()[0], DDI_3_2_XPATHS),
        entry(DDI_2_5_XPATHS.getNamespace()[0], DDI_2_5_XPATHS),
        entry(NESSTAR_XPATHS.getNamespace()[0], NESSTAR_XPATHS)
    );

    /**
     * Get the XPaths for a given XML namespace
     *
     * @throws UnsupportedXMLNamespaceException if the namespace is unsupported
     */
    public static XPaths getXPaths(Namespace namespace) {
        var xpaths = XPATH_MAP.get(namespace);
        if (xpaths == null) {
            throw new UnsupportedXMLNamespaceException(namespace);
        }
        return xpaths;
    }
}
