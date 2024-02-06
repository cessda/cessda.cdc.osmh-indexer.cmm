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
import eu.cessda.pasc.oci.models.cmmstudy.Country;
import eu.cessda.pasc.oci.models.cmmstudy.Pid;
import eu.cessda.pasc.oci.models.cmmstudy.Publisher;
import eu.cessda.pasc.oci.models.cmmstudy.TermVocabAttributes;
import lombok.*;
import org.jdom2.Element;
import org.jdom2.Namespace;

import javax.annotation.Nullable;
import java.util.*;

import static eu.cessda.pasc.oci.parser.ParsingStrategies.termVocabAttributeStrategy;
import static eu.cessda.pasc.oci.parser.XMLMapper.*;
import static java.util.Map.entry;

/**
 * XPath constants used to extract metadata from DDI XML documents.
 */
@Builder
@EqualsAndHashCode
@Getter
@ToString
@With
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
    private final String distributorXPath;
    @Nullable
    private final String fileTxtLanguagesXPath;
    @Nullable
    private final String filenameLanguagesXPath;
    private final XMLMapper<Map<String, List<TermVocabAttributes>>> samplingXPath;
    private final String typeOfModeOfCollectionXPath;
    private final String relatedPublicationsXPath;
    @Nullable
    private final XMLMapper<Map<String, List<UniverseElement>>> universeXPath;

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
        .abstractXPath(new XMLMapper<>("//ddi:DDIInstance/s:StudyUnit/r:Abstract/r:Content", parseLanguageContentOfElement(Element::getTextTrim)))
        // Study title
        .titleXPath(new XMLMapper<>("//ddi:DDIInstance/s:StudyUnit/r:Citation/r:Title/r:String", parseLanguageContentOfElement(Element::getTextTrim)))
        // 'Access study' link (when @typeOfUserID attribute is "URLServiceProvider")
        .studyURLDocDscrXPath(new XMLMapper<>("//ddi:DDIInstance/s:StudyUnit/r:UserID", extractMetadataObjectListForEachLang(ParsingStrategies::uriStrategy)))
        // URL of the study description page at the SP website (when @typeOfUserID attribute is "URLServiceProvider")
        .studyURLStudyDscrXPath(new XMLMapper<>("//ddi:DDIInstance/s:StudyUnit/r:UserID", extractMetadataObjectListForEachLang(ParsingStrategies::uriStrategy)))
        // Study number/PID (when @typeOfUserID attribute is "StudyNumber") - TODO: Implement parsing for this
        //.pidStudyXPath("//ddi:DDIInstance/s:StudyUnit/r:UserID")
        // Study number/PID
        .pidStudyXPath(new XMLMapper<>("//ddi:DDIInstance/s:StudyUnit/r:Citation/r:InternationalIdentifier", extractMetadataObjectListForEachLang(ParsingStrategies::pidLifecycleStrategy)))
        // Creator/PI
        .creatorsXPath(new XMLMapper<>("//ddi:DDIInstance/s:StudyUnit/r:Citation/r:Creator", ParsingStrategies::creatorsStrategy))
        // Terms of data access
        .dataRestrctnXPath(new XMLMapper<>("//ddi:DDIInstance/s:StudyUnit/a:Archive/a:ArchiveSpecific/a:Item/a:Access/r:Description/r:Content", extractMetadataObjectListForEachLang(ParsingStrategies::nullableElementValueStrategy)))
        // Data collection period
        .dataCollectionPeriodsXPath(new XMLMapper<>("//ddi:DDIInstance/s:StudyUnit/d:DataCollection/d:CollectionEvent/d:DataCollectionDate", elementList -> getFirstEntry(ParsingStrategies::dataCollectionPeriodsLifecycleStrategy).apply(elementList).orElse(
            new CMMStudyMapper.ParseResults<>(
                new CMMStudyMapper.DataCollectionPeriod(null, 0, null, Collections.emptyMap()),
                Collections.emptyList()
            )
        )))
        // Publication year
        .yearOfPubXPath(new XMLMapper<>("//ddi:DDIInstance/s:StudyUnit/r:Citation/r:PublicationDate/r:SimpleDate", getFirstEntry(Element::getTextTrim)))
        // Topics
        .classificationsXPath(new XMLMapper<>("//ddi:DDIInstance/s:StudyUnit/r:Coverage/r:TopicalCoverage/r:Subject", extractMetadataObjectListForEachLang(ParsingStrategies::termVocabAttributeLifecycleStrategy)))
        // Keywords
        .keywordsXPath(new XMLMapper<>("//ddi:DDIInstance/s:StudyUnit/r:Coverage/r:TopicalCoverage/r:Keyword", extractMetadataObjectListForEachLang(ParsingStrategies::termVocabAttributeLifecycleStrategy)))
        // Time dimension
        .typeOfTimeMethodXPath(new XMLMapper<>("//ddi:DDIInstance/s:StudyUnit/d:DataCollection/d:Methodology/d:TimeMethod/d:TypeOfTimeMethod", extractMetadataObjectListForEachLang(ParsingStrategies::termVocabAttributeLifecycleStrategy)))
        // Country
        .studyAreaCountriesXPath(new XMLMapper<>("//ddi:DDIInstance/s:StudyUnit/r:Coverage/r:SpatialCoverage/r:GeographicLocationReference", elementList -> {
            var countryListMap = new HashMap<String, List<Country>>();
            for (var element : elementList) {
              resolveReference(element).ifPresent(referencedElement -> {
                  var geographicReference = referencedElement.element();
                  var countryMap = ParsingStrategies.geographicReferenceStrategy(geographicReference);
                  countryMap.forEach((key, value) -> countryListMap.computeIfAbsent(key, k -> new ArrayList<>()).add(value));
              });
            }
            return countryListMap;
        }))
        // Analysis unit
        .unitTypeXPath(new XMLMapper<>("//ddi:DDIInstance/s:StudyUnit/r:AnalysisUnit", ParsingStrategies::analysisUnitStrategy))
        // Publisher
        .publisherXPath(new XMLMapper<>("//ddi:DDIInstance/s:StudyUnit/r:Citation/r:Publisher/r:PublisherReference", elementList -> {

            for (var element : elementList) {
                var referencedElement = resolveReference(element);
                var publisherMapOpt = referencedElement.flatMap(r -> switch (r.type()) {
                    case "Individual" -> Optional.of(ParsingStrategies.individualStrategy(r.element()));
                    case "Organization" -> Optional.of(ParsingStrategies.organizationStrategy(r.element()));
                    default -> Optional.empty();
                });
                if (publisherMapOpt.isPresent()) {
                    return publisherMapOpt.get();
                }
            }

            return Collections.emptyMap();
        }))
        // Language of data file(s)
        .fileTxtLanguagesXPath("//ddi:DDIInstance/r:ResourcePackage/pi:PhysicalInstance/r:Citation/r:Language")
        // Language-specific name of file
        .filenameLanguagesXPath("//ddi:DDIInstance/g:ResourcePackage/pi:PhysicalInstance/r:Citation/r:Title/r:String")
        // Sampling procedure
        .samplingXPath(new XMLMapper<>("//ddi:DDIInstance/s:StudyUnit/d:DataCollection/d:Methodology/d:SamplingProcedure", ParsingStrategies::samplingProceduresLifecycleStrategy))
        // Data collection mode
        .typeOfModeOfCollectionXPath("//ddi:DDIInstance/s:StudyUnit/d:DataCollection/d:CollectionEvent/d:ModeOfCollection/r:Description/r:Content")
        // PID of Related publication
        .relatedPublicationsXPath("//ddi:DDIInstance/s:StudyUnit/r:OtherMaterial/r:Citation/r:InternationalIdentifier/r:IdentifierContent")
        // Study description language
        .recordDefaultLanguage("//ddi:DDIInstance/@xml:lang")
        // Description of population
        .universeXPath(new XMLMapper<>("//ddi:DDIInstance/s:StudyUnit/c:ConceptualComponent/c:UniverseScheme/c:Universe", ParsingStrategies::universeLifecycleStrategy))
        .build();

    public Optional<XMLMapper<Map<String, String>>> getParTitleXPath() {
        return Optional.ofNullable(parTitleXPath);
    }

    public Optional<XMLMapper<Map<String, List<String>>>> getDataAccessUrlXPath() {
        return Optional.ofNullable(dataAccessUrlXPath);
    }

    public Optional<String> getFileTxtLanguagesXPath() {
        return Optional.ofNullable(fileTxtLanguagesXPath);
    }

    public Optional<String> getFilenameLanguagesXPath() {
        return Optional.ofNullable(filenameLanguagesXPath);
    }

    public Optional<XMLMapper<Map<String, List<String>>>> getStudyURLDocDscrXPath() {
        return Optional.ofNullable(studyURLDocDscrXPath);
    }

    public Optional<XMLMapper<Map<String, List<UniverseElement>>>> getUniverseXPath() {
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
        .distributorXPath("//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:distrbtr")
        // Language of data file(s)
        .fileTxtLanguagesXPath("//ddi:codeBook/ddi:fileDscr/ddi:fileTxt/@xml:lang")
        // Language-specific name of file
        .filenameLanguagesXPath("//ddi:codeBook/ddi:fileDscr/ddi:fileTxt/ddi:fileName/@xml:lang")
        // Sampling procedure
        .samplingXPath(new XMLMapper<>("//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:sampProc", elementList -> ParsingStrategies.conceptStrategy(elementList, ParsingStrategies::samplingTermVocabAttributeStrategy)))
        // Data collection mode
        .typeOfModeOfCollectionXPath("//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:collMode")
        // Related publication
        .relatedPublicationsXPath("//ddi:codeBook/ddi:stdyDscr/ddi:othrStdyMat/ddi:relPubl")
        // Study description language
        .recordDefaultLanguage("//ddi:codeBook/@xml:lang")
        // Description of population
        .universeXPath(new XMLMapper<>("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:universe", extractMetadataObjectListForEachLang(ParsingStrategies::universeStrategy)))
        .build();

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
        });
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
        .distributorXPath("//ddi:codeBook/stdyDscr/citation/distStmt/distrbtr")
        .samplingXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/method/dataColl/sampProc", elementList -> ParsingStrategies.conceptStrategy(elementList, ParsingStrategies::samplingTermVocabAttributeStrategy)))
        .typeOfModeOfCollectionXPath("//ddi:codeBook/stdyDscr/method/dataColl/collMode")
        .relatedPublicationsXPath("//ddi:codeBook/stdyDscr/othrStdyMat/relPubl")
        .universeXPath(new XMLMapper<>("//ddi:codeBook/stdyDscr/stdyInfo/sumDscr/universe", extractMetadataObjectListForEachLang(ParsingStrategies::universeStrategy)))
        .build();

    private final XMLMapper<Map<String, List<String>>> creatorsXPath;

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
