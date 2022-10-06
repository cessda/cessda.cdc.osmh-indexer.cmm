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

import lombok.*;
import org.jdom2.Namespace;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Optional;

/**
 * XPath constants used to extract metadata from DDI XML documents.
 */
@Builder
@EqualsAndHashCode
@Getter
@ToString
public final class XPaths implements Serializable {
    private static final long serialVersionUID = -6226660931460780008L;

    @NonNull
    private final Namespace ddiNS;
    private final Namespace[] oaiAndDdiNs;
    // Codebook Paths
    private final String recordDefaultLanguage;
    private final String yearOfPubXPath;
    private final String abstractXPath;
    private final String titleXPath;
    private final String parTitleXPath;
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
    @Nullable
    private final String universeXPath;

    public Optional<String> getStudyURLDocDscrXPath() {
        return Optional.ofNullable(studyURLDocDscrXPath);
    }

    public Optional<String> getFileTxtLanguagesXPath() {
        return Optional.ofNullable(fileTxtLanguagesXPath);
    }

    public Optional<String> getFilenameLanguagesXPath() {
        return Optional.ofNullable(filenameLanguagesXPath);
    }

    public Optional<String> getUniverseXPath() {
        return Optional.ofNullable(universeXPath);
    }

    private static final Namespace DDI_NS = Namespace.getNamespace("ddi", "ddi:codebook:2_5");

    /**
     * XPaths needed to extract metadata from DDI 2.5 documents.
     */
    public static final XPaths DDI_2_5_XPATHS = XPaths.builder()
        .ddiNS(DDI_NS)
        .oaiAndDdiNs(new Namespace[]{OaiPmhConstants.OAI_NS, DDI_NS})
        .abstractXPath("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:abstract")
        .titleXPath("//ddi:codeBook//ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:titl")
        .parTitleXPath("//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:parTitl")
        .studyURLDocDscrXPath("//ddi:codeBook/ddi:docDscr/ddi:citation/ddi:holdings")
        .studyURLStudyDscrXPath("//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:holdings")
        .pidStudyXPath("//ddi:codeBook//ddi:stdyDscr/ddi:citation/ddi:titlStmt/ddi:IDNo")
        .creatorsXPath("//ddi:codeBook//ddi:stdyDscr/ddi:citation/ddi:rspStmt/ddi:AuthEnty")
        .dataRestrctnXPath("//ddi:codeBook//ddi:stdyDscr/ddi:dataAccs/ddi:useStmt/ddi:restrctn")
        .dataCollectionPeriodsXPath("//ddi:codeBook//ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:collDate")
        .yearOfPubXPath("//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:distDate[1]/@date")
        .classificationsXPath("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:subject/ddi:topcClas")
        .keywordsXPath("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:subject/ddi:keyword")
        .typeOfTimeMethodXPath("//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:timeMeth")
        .studyAreaCountriesXPath("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:nation")
        .unitTypeXPath("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:anlyUnit")
        .publisherXPath("//ddi:codeBook/ddi:docDscr/ddi:citation/ddi:prodStmt/ddi:producer")
        .distributorXPath("//ddi:codeBook/ddi:stdyDscr/ddi:citation/ddi:distStmt/ddi:distrbtr")
        .fileTxtLanguagesXPath("//ddi:codeBook/ddi:fileDscr/ddi:fileTxt/@xml:lang")
        .filenameLanguagesXPath("//ddi:codeBook/ddi:fileDscr/ddi:fileTxt/ddi:fileName/@xml:lang")
        .samplingXPath("//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:sampProc")
        .typeOfModeOfCollectionXPath("//ddi:codeBook/ddi:stdyDscr/ddi:method/ddi:dataColl/ddi:collMode")
        .recordDefaultLanguage("//ddi:codeBook/@xml:lang")
        .universeXPath("//ddi:codeBook/ddi:stdyDscr/ddi:stdyInfo/ddi:sumDscr/ddi:universe")
        .build();

    private static final Namespace NESSTAR_DDI_NS = Namespace.getNamespace("ddi", "http://www.icpsr.umich.edu/DDI");

    /**
     * XPaths needed to extract metadata from NESSTAR flavoured DDI 1.2.2 documents.
     */
    public static final XPaths NESSTAR_XPATHS = XPaths.builder()
        .ddiNS(NESSTAR_DDI_NS)
        .oaiAndDdiNs(new Namespace[]{OaiPmhConstants.OAI_NS, NESSTAR_DDI_NS})
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
        .build();
}
