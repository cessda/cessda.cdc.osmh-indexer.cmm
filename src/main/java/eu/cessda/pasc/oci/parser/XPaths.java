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

import eu.cessda.pasc.oci.exception.UnsupportedXMLNamespaceException;
import lombok.*;
import org.jdom2.Namespace;

import javax.annotation.Nullable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;

/**
 * XPath constants used to extract metadata from DDI XML documents.
 */
@Builder
@EqualsAndHashCode
@Getter
@ToString
@With
public final class XPaths implements Serializable {
    @Serial
    private static final long serialVersionUID = -6226660931460780008L;

    @NonNull
    private final Namespace[] namespace;
    // Codebook Paths
    private final String recordDefaultLanguage;
    private final String yearOfPubXPath;
    private final String abstractXPath;
    private final String titleXPath;
    private final String parTitleXPath;
    @Nullable
    private final String dataAccessUrlXPath;
    @Nullable
    private final String studyURLDocDscrXPath;
    private final String studyURLStudyDscrXPath;
    private final String pidStudyXPath;
    private final String creatorsXPath;
    private final String dataRestrctnXPath;
    private final String dataCollectionPeriodsXPath;
    private final String classificationsXPath;
    private final String keywordsXPath;
    private final String typeOfTimeMethodXPath;
    private final String studyAreaCountriesXPath;
    private final String unitTypeXPath;
    private final String publisherXPath;
    private final String distributorXPath;
    @Nullable
    private final String fileTxtLanguagesXPath;
    @Nullable
    private final String filenameLanguagesXPath;
    private final String samplingXPath;
    private final String typeOfModeOfCollectionXPath;
    private final String relatedPublicationsXPath;
    @Nullable
    private final String universeXPath;

    public Optional<String> getDataAccessUrlXPath() {
        return Optional.ofNullable(dataAccessUrlXPath);
    }

    public Optional<String> getFileTxtLanguagesXPath() {
        return Optional.ofNullable(fileTxtLanguagesXPath);
    }

    public Optional<String> getFilenameLanguagesXPath() {
        return Optional.ofNullable(filenameLanguagesXPath);
    }

    public Optional<String> getStudyURLDocDscrXPath() {
        return Optional.ofNullable(studyURLDocDscrXPath);
    }

    public Optional<String> getUniverseXPath() {
        return Optional.ofNullable(universeXPath);
    }

    /**
     * XPaths needed to extract metadata from DDI 3.2 documents.
     */
    // TODO: Add parsing implementation to this file
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
        .abstractXPath("//ddi:DDIInstance/s:StudyUnit/r:Abstract/r:Content")
        // Study title
        .titleXPath("//ddi:DDIInstance/s:StudyUnit/r:Citation/r:Title/r:String")
        // Study title (in additional languages)
        .parTitleXPath("//ddi:DDIInstance/s:StudyUnit/r:Citation/r:Title/r:String")
        // 'Access study' link (when @typeOfUserID attribute is "URLServiceProvider")
        .studyURLDocDscrXPath("//ddi:DDIInstance/s:StudyUnit/r:UserID")
        // URL of the study description page at the SP website (when @typeOfUserID attribute is "URLServiceProvider")
        .studyURLStudyDscrXPath("//ddi:DDIInstance/s:StudyUnit/r:UserID")
        // Study number/PID (when @typeOfUserID attribute is "StudyNumber") - TODO: Implement parsing for this
        //.pidStudyXPath("//ddi:DDIInstance/s:StudyUnit/r:UserID")
        // Study number/PID 
        .pidStudyXPath("//ddi:DDIInstance/s:StudyUnit/r:Citation/r:InternationalIdentifier/r:IdentifierContent")
        // Creator/PI
        .creatorsXPath("//ddi:DDIInstance/r:Citation/r:Creator/r:CreatorName/r:String")
        // Terms of data access
        .dataAccessUrlXPath("//ddi:DDIInstance/s:StudyUnit/a:Archive/a:ArchiveSpecific/a:Item/a:Access/r:Description/r:Content")
        // Terms of data access
        .dataRestrctnXPath("//ddi:DDIInstance/s:StudyUnit/a:Archive/a:ArchiveSpecific/a:Item/a:Access/r:Description/r:Content")
        // Data collection period
        .dataCollectionPeriodsXPath("//ddi:DDIInstance/s:StudyUnit/d:DataCollection/d:CollectionEvent/d:CollectionSituation/r:Description/r:Content")
        // Publication year
        .yearOfPubXPath("//ddi:DDIInstance/s:StudyUnit/r:Citation/r:PublicationDate/r:SimpleDate")
        // Topics
        .classificationsXPath("//ddi:DDIInstance/s:StudyUnit/r:Coverage/r:TopicalCoverage/r:Subject")
        // Keywords
        .keywordsXPath("//ddi:DDIInstance/s:StudyUnit/r:Coverage/r:TopicalCoverage/r:Keyword")
        // Time dimension
        .typeOfTimeMethodXPath("//ddi:DDIInstance/s:StudyUnit/d:DataCollection/d:Methodology/d:TimeMethod/r:Description/r:Content")
        // Country
        .studyAreaCountriesXPath("//ddi:DDIInstance/s:StudyUnit/r:Coverage/r:SpatialCoverage/r:Description/r:Content")
        // Analysis unit
        .unitTypeXPath("//ddi:DDIInstance/s:StudyUnit/r:AnalysisUnitsCovered")
        // Publisher
        .publisherXPath("//ddi:DDIInstance/s:StudyUnit/r:Citation/r:Publisher/r:PublisherReference")
        // Publisher Reference (Institution)
        .distributorXPath("//ddi:DDIInstance/s:StudyUnit/r:Citation/r:Publisher/r:PublisherReference/r:URN")
        // Language of data file(s)
        .fileTxtLanguagesXPath("//ddi:DDIInstance/r:ResourcePackage/pi:PhysicalInstance/r:Citation/r:Language")
        // Language-specific name of file
        .filenameLanguagesXPath("//ddi:DDIInstance/g:ResourcePackage/pi:PhysicalInstance/r:Citation/r:Title/r:String")
        // Sampling procedure
        .samplingXPath("//ddi:DDIInstance/s:StudyUnit/d:DataCollection/d:Methodology/d:SamplingProcedure/r:Description/r:Content")
        // Data collection mode
        .typeOfModeOfCollectionXPath("//ddi:DDIInstance/s:StudyUnit/d:DataCollection/d:CollectionEvent/d:ModeOfCollection/r:Description/r:Content")
        // PID of Related publication
        .relatedPublicationsXPath("//ddi:DDIInstance/s:StudyUnit/r:OtherMaterial/r:Citation/r:InternationalIdentifier/r:IdentifierContent")
        // Study description language
        .recordDefaultLanguage("//ddi:DDIInstance/@xml:lang")
        // Description of population - TODO: Implement inclusion/exclusion
        .universeXPath("//ddi:DDIInstance/s:StudyUnit/c:ConceptualComponent/c:UniverseScheme")
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
     * XPaths needed to extract metadata from DDI 2.5 documents.
     */
    public static final XPaths DDI_2_5_XPATHS = XPaths.builder()
        .namespace(new Namespace[]{ Namespace.getNamespace("ddi", "ddi:codebook:2_5") })
        // Abstract
        .abstractXPath("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:abstract")
        // Study title
        .titleXPath("//ddi:codeBook//ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:titl")
        // Study title (in additional languages)
        .parTitleXPath("//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:parTitl")
        // 'Access study' link
        .studyURLDocDscrXPath("//ddi:codeBook/ddi:docDscr/ddi:citation/ddi:holdings")
        // URL of the study description page at the SP website
        .studyURLStudyDscrXPath("//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:holdings")
        // Study number/PID
        .pidStudyXPath("//ddi:codeBook//ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:IDNo")
        // Creator
        .creatorsXPath("//ddi:codeBook//ddi:stdyDscr/ddi:citation/ddi:rspStmt/ddi:AuthEnty")
        // Terms of data access
        .dataAccessUrlXPath("//ddi:codeBook//ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:specPerm")
        // Terms of data access
        .dataRestrctnXPath("//ddi:codeBook//ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:restrctn")
        // Data collection period
        .dataCollectionPeriodsXPath("//ddi:codeBook//ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:collDate")
        // Publication year
        .yearOfPubXPath("//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:distDate[1]/@date")
        // Topics
        .classificationsXPath("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:subject/ddi:topcClas")
        // Keywords
        .keywordsXPath("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:subject/ddi:keyword")
        // Time dimension
        .typeOfTimeMethodXPath("//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:timeMeth")
        // Country
        .studyAreaCountriesXPath("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:nation")
        // Analysis unit
        .unitTypeXPath("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:anlyUnit")
        // Publisher name/Contributor
        .publisherXPath("//ddi:codeBook/ddi:docDscr/ddi:citation/ddi:prodStmt/ddi:producer")
        // Publisher
        .distributorXPath("//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:distrbtr")
        // Language of data file(s)
        .fileTxtLanguagesXPath("//ddi:codeBook/ddi:fileDscr/ddi:fileTxt/@xml:lang")
        // Language-specific name of file
        .filenameLanguagesXPath("//ddi:codeBook/ddi:fileDscr/ddi:fileTxt/ddi:fileName/@xml:lang")
        // Sampling procedure
        .samplingXPath("//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:sampProc")
        // Data collection mode
        .typeOfModeOfCollectionXPath("//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:collMode")
        // Related publication
        .relatedPublicationsXPath("//ddi:codeBook/ddi:stdyDscr/ddi:othrStdyMat/ddi:relPubl")
        // Study description language
        .recordDefaultLanguage("//ddi:codeBook/@xml:lang")
        // Description of population
        .universeXPath("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:universe")
        .build();

    /**
     * XPaths needed to extract metadata from NESSTAR flavoured DDI 1.2.2 documents.
     */
    public static final XPaths NESSTAR_XPATHS = XPaths.builder()
        .namespace(new Namespace[]{ Namespace.getNamespace("ddi", "http://www.icpsr.umich.edu/DDI") })
        .recordDefaultLanguage("//ddi:codeBook/@xml-lang") // Nesstar with "-"
        // Closest for Nesstar based on CMM mapping doc but the above existing one for ddi2.5 seems to be present in Nesstar
        .yearOfPubXPath("//ddi:codeBook/stdyDscr/citation/distStmt/distDate[1]/@date")
        .abstractXPath("//ddi:codeBook/stdyDscr/stdyInfo/abstract")
        .titleXPath("//ddi:codeBook/stdyDscr/citation/titlStmt/titl")
        .parTitleXPath("//ddi:codeBook/stdyDscr/citation/titlStmt/parTitl")
        .studyURLStudyDscrXPath("//ddi:codeBook/stdyDscr/dataAccs/setAvail/accsPlac")
        // PID path missing for most nesstar repos. Available in FORS but:
        //  -No agency
        //  -Only element freetext value.
        .pidStudyXPath("//ddi:codeBook/stdyDscr/citation/titlStmt/IDNo") // use @agency instead?
        .creatorsXPath("//ddi:codeBook/stdyDscr/citation/rspStmt/AuthEnty")
        .dataRestrctnXPath("//ddi:codeBook/stdyDscr/dataAccs/useStmt/restrctn")
        .dataCollectionPeriodsXPath("//ddi:codeBook/stdyDscr/stdyInfo/sumDscr/collDate")
        .classificationsXPath("//ddi:codeBook/stdyDscr/stdyInfo/subject/topcClas")
        .keywordsXPath("//ddi:codeBook/stdyDscr/stdyInfo/subject/keyword")
        .typeOfTimeMethodXPath("//ddi:codeBook/stdyDscr/method/dataColl/timeMeth")
        .studyAreaCountriesXPath("//ddi:codeBook/stdyDscr/stdyInfo/sumDscr/nation")
        .unitTypeXPath("//ddi:codeBook/stdyDscr/stdyInfo/sumDscr/anlyUnit")
        .publisherXPath("//ddi:codeBook/docDscr/citation/prodStmt/producer")
        .distributorXPath("//ddi:codeBook/stdyDscr/citation/distStmt/distrbtr")
        .samplingXPath("//ddi:codeBook/stdyDscr/method/dataColl/sampProc")
        .typeOfModeOfCollectionXPath("//ddi:codeBook/stdyDscr/method/dataColl/collMode")
        .relatedPublicationsXPath("//ddi:codeBook/stdyDscr/othrStdyMat/relPubl")
        .universeXPath("//ddi:codeBook/stdyDscr/stdyInfo/sumDscr/universe")
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
