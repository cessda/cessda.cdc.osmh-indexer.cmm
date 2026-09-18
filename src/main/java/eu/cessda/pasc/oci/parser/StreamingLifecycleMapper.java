/*
 * Copyright © 2017-2025 CESSDA ERIC (support@cessda.eu)
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

import eu.cessda.pasc.oci.LoggingConstants;
import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import eu.cessda.pasc.oci.configurations.Repo;
import eu.cessda.pasc.oci.models.cmmstudy.*;
import eu.cessda.pasc.oci.models.cmmstudy.Creator;
import eu.cessda.pasc.oci.models.cmmstudy.Publisher;
import eu.cessda.pasc.oci.models.lifecycle.*;
import eu.cessda.pasc.oci.models.lifecycle.Universe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.format.DateTimeParseException;
import java.util.*;

import static eu.cessda.pasc.oci.parser.ParsingStrategies.parseDataAccessString;
import static net.logstash.logback.argument.StructuredArguments.value;

@Component
@Slf4j
public class StreamingLifecycleMapper {

    private final AppConfigurationProperties.OaiPmh oaiPmh;

    public StreamingLifecycleMapper() {
        this.oaiPmh =  new AppConfigurationProperties.OaiPmh(
            new AppConfigurationProperties.MetadataParsingDefaultLang(
                    true,
                    "en"
            ),
            "<br>"
        );
    }

    @Autowired
    public StreamingLifecycleMapper(AppConfigurationProperties appConfigurationProperties) {
        this.oaiPmh = appConfigurationProperties.oaiPmh();
    }

    CMMStudy parseFragmentedStudy(Repo repository, URI uri, String studyNumber, String lastModified, Map<ObjectInformation, DDIObject> components, StudyUnit studyUnit) {

        String defaultLang = repository.defaultLanguage();
        if (defaultLang == null) {
            defaultLang = oaiPmh.metadataParsingDefaultLang().lang();
        }

        /*
         * Abstract
         */
        var abstr = studyUnit.abstractMap();

        /*
         * Study Area Countries
         */
        Map<String, List<Country>> studyAreaCountries = new HashMap<>();


        /*
         * Classifications
         */
        Map<String, List<TermVocabAttributes>> classifications = Collections.emptyMap();


        /*
         * Keywords
         */
        Map<String, List<TermVocabAttributes>> keywords = Collections.emptyMap();

        Coverage coverage = studyUnit.coverage();
        if (coverage != null) {
            var topicalCoverage = coverage.topicalCoverage();
            if (topicalCoverage != null) {
                classifications = extractMultilingualCVMap(topicalCoverage.subjects(), defaultLang);
                keywords = extractMultilingualCVMap(topicalCoverage.keywords(), defaultLang);
            }

            var spatialCoverage = coverage.spatialCoverage();
            if (spatialCoverage != null) {
                for (var geoRef : spatialCoverage.geographicLocatonReferenceList()) {
                    var referencedObject = components.get(geoRef.objInf());

                    if (referencedObject instanceof GeographicLocation geographicLocation) {
                        var locationValue = geographicLocation.locationValue();
                        locationValue.locationValueName().forEach((lang, string) -> {
                            var country = new Country(locationValue.geographicLocationIdentifier(), string);
                            studyAreaCountries.computeIfAbsent(lang, k -> new ArrayList<>()).add(country);
                        });
                    }
                }
            }
        }

        /*
         * Citation related
         */
        var citation = studyUnit.citation();

        Map<String, List<eu.cessda.pasc.oci.models.cmmstudy.Creator>> creatorMap = Collections.emptyMap();
        Map<String, List<Pid>> pidMap = Collections.emptyMap();
        Map<String, Publisher> publisherMap = Collections.emptyMap();

        String publicationYear = null;
        Map<String, String> titleStudy = Collections.emptyMap();

        if (citation != null) {
            var citationResult = parseCitation(components, citation);
            creatorMap = citationResult.creatorMap();
            pidMap = citationResult.pidMap();
            publisherMap = citationResult.publisherMap();
            publicationYear = citationResult.publicationYear();
            titleStudy = citationResult.titleStudy();
        }

        /*
         * Data access - includes free texts and URLs
         */
        var archiveResult = parseArchiveReference(components, studyUnit);

        String dataAccess = archiveResult.dataAccess();
        Map<String, List<String>> dataAccessFreeTexts = archiveResult.dataAccessFreeTexts();

        /*
         * Data Access URL
         */
        // Not needed for lifecycle document
        var dataAccessUrl = Collections.<String, URI>emptyMap();

        /*
         * Data Collection
         */

        var dataCollectionResult = getDataCollectionResult(components, defaultLang, studyUnit);

        var typeOfSamplingProcedures = dataCollectionResult.typeOfSamplingProcedures();
        var typeOfTimeMethods = Map.of(defaultLang, dataCollectionResult.timeMethodList());
        var typeOfModeOfCollections = Map.of(defaultLang, dataCollectionResult.typeOfModeOfCollectionsList());

        /*
         * Data Kind
         */
        var dataKindFreeTextList = parseKindOfData(studyUnit);
        Map<String, List<DataKindFreeText>> dataKindFreeTexts = Map.of(defaultLang, dataKindFreeTextList);

        /*
         * File Languages
         */
        var fileLanguages = parsePhysicalInstance(components, studyUnit);

        /*
         * Funding
         */
        var funding = parseFunding(components, defaultLang, studyUnit);

        /*
         * General Data Formats
         */
        var generalDataFormatList = parseGeneralDataFormats(studyUnit);
        Map<String, List<TermVocabAttributes>> generalDataFormats = Map.of(defaultLang, generalDataFormatList);


        /*
         * Related Publications
         */
        var relatedPublications = parseOtherMaterials(components, studyUnit);

        /*
         * Series
         */
        Map<String, List<Series>> series = Collections.emptyMap();
        var seriesStatement = studyUnit.seriesStatement();
        if (seriesStatement != null) {
            try {
                series = parseSeriesStatement(seriesStatement);
            } catch (URISyntaxException e) {
                log.warn("[{}] URLs in the Series Statement in study {} couldn't be parsed: {}",
                        value(LoggingConstants.REPO_NAME, repository.code()),
                        value(LoggingConstants.STUDY_ID, studyNumber),
                        e.toString()
                );
            }
        }

        /*
         * Study URL
         */
        var studyUrl = Collections.<String, URI>emptyMap();

        /*
         * Unit Types
         */
        var unitTypes = parseAnalysisUnit(studyUnit);
        var unitTypeMap = Map.of(defaultLang, unitTypes);

        /*
         * Universe
         */
        var universeMap = parseUniverse(components, studyUnit);

        // Universe can also be part of a ConceptualComponent
        //studyUnit.conceptualComponent().universeScheme().universe()

        // Extract data collection parse results
        var dataCollectionResults = dataCollectionResult.dataCollectionParseResult().results();

        URI studyXmlSourceUrl = null;
        try {
            //should retrieve from header, if present
            studyXmlSourceUrl = OaiPmhHelpers.buildGetStudyFullUrl(repository.url(), studyNumber, repository.preferredMetadataParam());
        } catch (URISyntaxException e) {
            log.warn("[{}] Study URL for {} couldn't be parsed: {}",
                    value(LoggingConstants.REPO_NAME, repository.code()),
                    value(LoggingConstants.STUDY_ID, studyNumber),
                    e.toString()
            );
        }

        return new CMMStudy(
                abstr,
                classifications,
                creatorMap,
                dataAccess,
                dataAccessFreeTexts,
                dataAccessUrl,
                dataCollectionResults.getStartDate().orElse(null),
                dataCollectionResults.getEndDate().orElse(null),
                dataCollectionResults.getDataCollectionYear().orElse(null),
                dataCollectionResults.getFreeTexts(),
                dataKindFreeTexts,
                fileLanguages,
                funding,
                generalDataFormats,
                keywords,
                pidMap,
                publicationYear,
                publisherMap,
                relatedPublications,
                series,
                studyAreaCountries,
                studyNumber,
                studyUrl,
                typeOfModeOfCollections,
                titleStudy,
                typeOfTimeMethods,
                typeOfSamplingProcedures,
                unitTypeMap,
                universeMap,
                lastModified,
                studyXmlSourceUrl,
                uri
        );
    }

    private static Map<String, eu.cessda.pasc.oci.models.cmmstudy.Universe> parseUniverse(Map<ObjectInformation, DDIObject> components, StudyUnit studyUnit) {
        var universeMap = new HashMap<String, eu.cessda.pasc.oci.models.cmmstudy.Universe>();
        Map<String, UniverseElement> uElemMap = new HashMap<>();

        // Universe can be directly referenced in the StudyUnit
        var universeReference = studyUnit.universe();
        if (universeReference != null) {
            DDIObject ddiObject = components.get(universeReference.objInf());
            if (ddiObject instanceof Universe universe) {
                eu.cessda.pasc.oci.models.cmmstudy.Universe.Clusion inclusionStatus;
                if (universe.inclusive()) {
                    inclusionStatus = eu.cessda.pasc.oci.models.cmmstudy.Universe.Clusion.I;
                } else {
                    inclusionStatus = eu.cessda.pasc.oci.models.cmmstudy.Universe.Clusion.E;
                }

                // Default to label
                universe.label().forEach((lang, name) ->
                        uElemMap.put(lang, new UniverseElement(inclusionStatus, name))
                );

                // Fallback to name if label is not present
                universe.universeName().forEach((lang, name) ->
                        uElemMap.putIfAbsent(lang, new UniverseElement(inclusionStatus, name))
                );
            }
        }

        // Convert uElemMap to universeMap
        uElemMap.forEach((lang, uElem) -> {
            switch (uElem.clusion()) {
                case I -> universeMap.put(lang, new eu.cessda.pasc.oci.models.cmmstudy.Universe(uElem.content(), null));
                case E -> universeMap.put(lang, new eu.cessda.pasc.oci.models.cmmstudy.Universe(null, uElem.content()));
            }
        });

        return universeMap;
    }

    private static List<TermVocabAttributes> parseAnalysisUnit(StudyUnit studyUnit) {
        var unitTypes = new ArrayList<TermVocabAttributes>();
        for (var analysisUnit : studyUnit.analysisUnit()) {
            var vocab = analysisUnit.name();
            var vocabUri = analysisUnit.urn();
            var id = analysisUnit.id();
            var term = analysisUnit.content();
            var type = new TermVocabAttributes(vocab, vocabUri, id, term);
            unitTypes.add(type);
        }
        return unitTypes;
    }

    private static Map<String, List<Series>> parseSeriesStatement(SeriesStatement seriesStatement) throws URISyntaxException {
        var series = new HashMap<String, List<Series>>();

        // Discover all languages
        var langSet = new HashSet<>(seriesStatement.seriesDescription().keySet());
        langSet.addAll(seriesStatement.seriesName().keySet());

        if (langSet.isEmpty()) {
            var uris = new ArrayList<URI>();
            for (var uriString : seriesStatement.seriesRepositoryLocation()) {
                uris.add(new URI(uriString));
            }

            // URI only series object, set lang to *
            var seriesObj = new Series(Collections.emptyList(), Collections.emptyList(), uris);
            series.computeIfAbsent("*", k -> new ArrayList<>()).add(seriesObj);
        }

        // URIs are added to all valid languages
        for (var lang : langSet) {
            var name = seriesStatement.seriesName().get(lang);
            var descriptions = seriesStatement.seriesDescription().get(lang);

            var uris = new ArrayList<URI>();
            for (var uriString : seriesStatement.seriesRepositoryLocation()) {
                uris.add(new URI(uriString));
            }

            // Null check of descriptions
            List<String> descriptionList = Collections.emptyList();
            if (descriptions != null) {
                descriptionList = Collections.singletonList(descriptions);
            }

            var seriesObj = new Series(name, descriptionList, uris);
            series.computeIfAbsent(lang, k -> new ArrayList<>()).add(seriesObj);
        }

        return series;
    }

    private static Map<String, List<RelatedPublication>> parseOtherMaterials(Map<ObjectInformation, DDIObject> components, StudyUnit studyUnit) {
        var relatedPublications = new HashMap<String, List<RelatedPublication>>();
        for (var reference : studyUnit.otherMaterialSchemeReferenceList()) {
            var referencedObject = components.get(reference.objInf());
            if (referencedObject instanceof OtherMaterialScheme scheme) {
                for (var otherMaterial : scheme.otherMaterialList()) {
                    var typeOfMaterial = otherMaterial.typeOfMaterial();
                    if (typeOfMaterial == null || !"Related Publication".equals(typeOfMaterial.content())) {
                        continue;
                    }

                    Map<String, String> titleMap = Collections.emptyMap();
                    List<URI> uriList = new ArrayList<>();
                    String publicationDate = null;

                    // First try ExternalURLReference as primary URI
                    for (var externalUrl : otherMaterial.externalURLReference()) {
                        uriList.add(URI.create(externalUrl));
                    }

                    // Extract the citation
                    var relPubCitation = otherMaterial.citation();
                    if (relPubCitation != null) {
                        var internationalIdentifier = relPubCitation.internationalIdentifier();
                        if (internationalIdentifier != null) {
                            var identifierContext = internationalIdentifier.identifierContent();
                            uriList.add(URI.create(identifierContext));
                        }

                        titleMap = relPubCitation.title();

                        if (relPubCitation.publicationDate() instanceof SimpleDateType(String date)) {
                            publicationDate = date;
                        }
                    }

                    for (Map.Entry<String, String> entry : titleMap.entrySet()) {
                        // Merge the language dependent title with the list of URIs and publication date
                        var relPub = new RelatedPublication(entry.getValue(), uriList, publicationDate);
                        relatedPublications.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(relPub);
                    }
                }
            }
        }
        return relatedPublications;
    }

    private static List<TermVocabAttributes> parseGeneralDataFormats(StudyUnit studyUnit) {
        var generalDataFormatList = new ArrayList<TermVocabAttributes>();
        for (var gdf : studyUnit.generalDataFormatList()) {
            var term = new TermVocabAttributes(gdf.name(), gdf.urn(), gdf.id(), gdf.content());
            generalDataFormatList.add(term);
        }
        return generalDataFormatList;
    }

    private static Map<String, List<Funding>> parseFunding(Map<ObjectInformation, DDIObject> components, String defaultLang, StudyUnit studyUnit) {
        var funding = new HashMap<String, List<Funding>>();
        for (var fundingInformation : studyUnit.fundingInformation()) {
            String grantNumber = fundingInformation.grantNumber();
            DDIObject referencedObject = null;

            var agencyOrganizationReference = fundingInformation.agencyOrganizationReference();
            if (agencyOrganizationReference != null) {
                referencedObject = components.get(agencyOrganizationReference.objInf());
            }

            if (referencedObject instanceof Organization organization) {
                organization.names().names().forEach((lang, name) -> {
                    var fundingElement = new Funding(grantNumber, name);
                    funding.computeIfAbsent(lang, k -> new ArrayList<>()).add(fundingElement);
                });
            } else if (grantNumber != null) {
                var fundingElement = new Funding(grantNumber, null);
                funding.computeIfAbsent(defaultLang, k -> new ArrayList<>()).add(fundingElement);
            }
        }
        return funding;
    }

    private static Set<String> parsePhysicalInstance(Map<ObjectInformation, DDIObject> components, StudyUnit studyUnit) {
        var fileLanguages = new HashSet<String>();
        for (var phyInstRef : studyUnit.physicalInstanceReference()) {
            // Get referenced PhysicalInstance
            var objInf = phyInstRef.objInf();
            var referencedObject = components.get(objInf);

            if (referencedObject instanceof PhysicalInstance physicalInstance) {
                // Get filenames from citation
                var physicalInstanceCitation = physicalInstance.citation();
                if (physicalInstanceCitation != null) {
                    //fileLanguages.addAll(citation.language());
                    fileLanguages.addAll(physicalInstanceCitation.title().keySet());
                }
            }
        }
        return fileLanguages;
    }

    private static List<DataKindFreeText> parseKindOfData(StudyUnit studyUnit) {
        var dataKindFreeTextList = new ArrayList<DataKindFreeText>();
        for (var kindOfData : studyUnit.kindOfData()) {
            var dataKind = new DataKindFreeText(kindOfData.controlledVocabulary().content(), kindOfData.type());
            dataKindFreeTextList.add(dataKind);
        }
        return dataKindFreeTextList;
    }

    private static DataCollectionResult getDataCollectionResult(Map<ObjectInformation, DDIObject> components, String defaultLang, StudyUnit studyUnit) {
        /*
         * Type of Mode of Collections
         */
        var typeOfModeOfCollectionsList = new ArrayList<TermVocabAttributes>();

        /*
         * Type of Time Methods
         */
        var timeMethodList = new ArrayList<TermVocabAttributes>();

        /*
         * Type of Sampling Procedures
         */
        var typeOfSamplingProcedures = new HashMap<String, List<TermVocabAttributes>>();

        /*
         * Data Collection
         */

        CMMStudyMapper.ParseResults<CMMStudyMapper.DataCollectionPeriod, DateTimeParseException> dataCollectionParseResult = XPaths.EMPTY_PARSE_RESULTS;

        for (var dataCollectionRef : studyUnit.dataCollectionReference()) {
            // Get referenced DataCollection
            var objInf = dataCollectionRef.objInf();
            DDIObject referencedObject = components.get(objInf);

            if (referencedObject instanceof DataCollection dataCollection) {
                var collectionEvent = dataCollection.collectionEvent();
                if (collectionEvent != null) {

                    String startDate = null;
                    String endDate = null;
                    Integer year = null;

                    DateTimeParseException parseException = null;

                    // TODO: select earliest/latest dates
                    if (!collectionEvent.isEmpty()) {
                        var event = collectionEvent.getFirst();
                        var collectionDate = event.collectionDate();

                        try {
                            if (collectionDate instanceof SimpleDateType(String simpleDate)) {
                                year = ParsingStrategies.parseDateIntoYear(simpleDate);
                            } else if (collectionDate instanceof PeriodDateType periodDateType) {
                                startDate = periodDateType.startDate();
                                endDate = periodDateType.endDate();
                                year = ParsingStrategies.parseDateIntoYear(startDate);
                            }
                        } catch (DateTimeParseException e) {
                            parseException = e;
                        }

                        var dataCollectionPeriod = new CMMStudyMapper.DataCollectionPeriod(startDate, year, endDate, Collections.emptyMap());
                        dataCollectionParseResult = new CMMStudyMapper.ParseResults<>(dataCollectionPeriod, parseException);
                    }


                    for (var event : collectionEvent) {
                        /*
                         * Type of Mode of Collections
                         */
                        for (var modeOfCollection : event.modesOfCollection()) {
                            var cv = modeOfCollection.cv();

                            if (cv != null) {
                                var term = new TermVocabAttributes(cv.name(), cv.urn(), cv.id(), cv.content());
                                typeOfModeOfCollectionsList.add(term);
                            }
                        }
                    }
                }

                DDIObject mReferencedObject = null;

                var methodologyReference = dataCollection.methodologyReference();
                if (methodologyReference != null) {
                    mReferencedObject = components.get(methodologyReference.objInf());
                }

                if (mReferencedObject instanceof Methodology methodology) {
                    for (var timeMethod : methodology.timeMethod()) {
                        var tm = timeMethod.typeOfTimeMethod();
                        if (tm != null) {
                            var term = new TermVocabAttributes(tm.name(), tm.urn(), tm.id(), tm.content());
                            timeMethodList.add(term);
                        }
                    }

                    var samplingProcedure = methodology.samplingProcedure();
                    if (samplingProcedure != null) {
                        var sp = samplingProcedure.typeOfSamplingProcedure();

//                        samplingProcedure.content().forEach((lang, string) -> {
//
//                            TermVocabAttributes term;
//                            if (sp != null) {
//                                term = new TermVocabAttributes(sp.name(), sp.urn(), sp.id(), string);
//                            } else {
//                                term = new TermVocabAttributes(null, null, null, string);
//                            }
//
//                            typeOfSamplingProcedures.computeIfAbsent(lang, k -> new ArrayList<>()).add(term);
//                        });

                        if (/* samplingProcedure.content().isEmpty() && */ sp != null) {
                            var term = new TermVocabAttributes(sp.name(), sp.urn(), sp.id(), sp.content());
                            typeOfSamplingProcedures.computeIfAbsent(defaultLang, k -> new ArrayList<>()).add(term);
                        }
                    }
                }
            }
        }

        return new DataCollectionResult(typeOfModeOfCollectionsList, timeMethodList, typeOfSamplingProcedures, dataCollectionParseResult);
    }

    private static ArchiveResult parseArchiveReference(Map<ObjectInformation, DDIObject> components, StudyUnit studyUnit) {
        String dataAccess = null;
        var dataAccessFreeTexts = new HashMap<String, List<String>>();

        for (var archiveRef : studyUnit.archiveReference()) {
            // Get referenced archive
            var objInf = archiveRef.objInf();
            DDIObject referencedObject = components.get(objInf);

            if (referencedObject instanceof Archive archive) {
                Access access = archive.access();
                if (access != null) {
                    if (dataAccess == null) {
                        dataAccess = parseDataAccess(access);
                    }

                    var accessDescription = access.accessDescription();
                    accessDescription.forEach((lang, description) ->
                            dataAccessFreeTexts.computeIfAbsent(lang, k -> new ArrayList<>()).add(description)
                    );
                }
            }
        }

        return new ArchiveResult(dataAccess, dataAccessFreeTexts);
    }

    private static String parseDataAccess(Access access) {
        String dataAccess = null;

        // Try parsing TypeOfAccess
        var typeOfAccess = access.typeOfAccess();
        if (typeOfAccess != null) {
            var parsedDataAccess = parseDataAccessString(typeOfAccess);
            if (parsedDataAccess != null) {
                dataAccess = parsedDataAccess;
            }
        }

        // Fall back to AccessTypeName if TypeOfAccess is not present or didn't return a result
        if (dataAccess != null) {
            var accessTypeName = access.accessTypeName();
            for (var accessType : accessTypeName.values()) {
                var parsedDataAccess = parseDataAccessString(accessType);
                if (parsedDataAccess != null) {
                    dataAccess = parsedDataAccess;
                    break;
                }
            }
        }

        return dataAccess;
    }

    @SuppressWarnings("java:S6880")
    private static CitationResult parseCitation(Map<ObjectInformation, DDIObject> components, Citation citation) {
        var creatorMap = new HashMap<String, List<Creator>>();
        var pidMap = new HashMap<String, List<Pid>>();
        var publisherMap = new HashMap<String, Publisher>();

        /*
         * Creators
         */
        var sourceCreator = citation.creator();

        // Try to resolve reference
        var creatorRef = sourceCreator.creatorReference();

        DDIObject referencedObject = null;
        if (creatorRef != null) {
            referencedObject = components.get(creatorRef.objInf());
        }

        if (referencedObject instanceof Organization organization) {
            organization.names().names().forEach((lang, name) -> {
                var crObj = new Creator(name, null, null);
                creatorMap.computeIfAbsent(lang, k -> new ArrayList<>()).add(crObj);
            });
        } else if (referencedObject instanceof Individual individual) {
            var individualIdentification = individual.individualIdentification();
            var researcherID = individualIdentification.researcherID();

            // Extract creator ID (e.g. ORCID)
            var creatorIdentifier = new Creator.Identifier(
                    researcherID.typeOfId(),
                    researcherID.researcherIdentification(),
                    URI.create(researcherID.uri()),
                    "pid"
            );

            individualIdentification.individualName().fullName().forEach((lang, name) -> {
                var crObj = new Creator(name, null, Collections.singletonList(creatorIdentifier));
                creatorMap.computeIfAbsent(lang, k -> new ArrayList<>()).add(crObj);
            });
        } else {
            // No reference, try extracting directly
            sourceCreator.creatorName().forEach((lang, creator) -> {
                var crObj = new Creator(creator, sourceCreator.affiliation(), null);
                creatorMap.computeIfAbsent(lang, k -> new ArrayList<>()).add(crObj);
            });
        }

        /*
         * PID
         */
        var internationalIdentifier = citation.internationalIdentifier();
        if (internationalIdentifier != null) {
            var agency = internationalIdentifier.managingAgency();
            var identifier = internationalIdentifier.identifierContent();

            var pid = new Pid(agency, identifier);

            pidMap.computeIfAbsent("*", k -> new ArrayList<>()).add(pid);
        }

        /*
         * Publication Date
         */
        String publicationYear = switch (citation.publicationDate()) {
            case SimpleDateType(String simpleDate) -> simpleDate;
            case PeriodDateType periodDateType -> periodDateType.startDate();
        };

        /*
         * Publisher
         */

        // Resolve publisher reference
        var publisher = citation.publisher();
        if (publisher != null) {
            var publisherReference = publisher.publisherReference();
            if (publisherReference != null) {
                DDIObject ddiObject = components.get(publisherReference.objInf());
                if (ddiObject instanceof Organization organization) {
                    var names = organization.names();
                    names.names().forEach((lang, name) -> {
                        // Get abbreviation, if exists
                        var abbr = names.abbreviations().get(lang);
                        var publisherObj = new Publisher(abbr, name);
                        publisherMap.put(lang, publisherObj);
                    });
                } else if (ddiObject instanceof Individual individual) {
                    var names = individual.individualIdentification().individualName().fullName();
                    names.forEach((lang, name) -> {
                        // Get abbreviation, if exists
                        var publisherObj = new Publisher(null, name);
                        publisherMap.put(lang, publisherObj);
                    });
                }
            }
        }

        return new CitationResult(publicationYear, citation.title(), creatorMap, pidMap, publisherMap);
    }

    private static Map<String, List<TermVocabAttributes>> extractMultilingualCVMap(Map<String, List<ControlledVocabulary>> topicalCoverage, String defaultLang) {
        var controlledVocabularyMap = new HashMap<String, List<TermVocabAttributes>>();
        topicalCoverage.forEach((lang, keywordsList) -> {
            for (var keyword : keywordsList) {
                var termVocabAttributes = new TermVocabAttributes(keyword.name(), keyword.urn(), keyword.id(), keyword.content());
                if (lang == null || lang.equals("*")) {
                    lang = defaultLang;
                }
                controlledVocabularyMap.computeIfAbsent(lang, k -> new ArrayList<>()).add(termVocabAttributes);
            }
        });
        return controlledVocabularyMap;
    }

    private record DataCollectionResult(
            List<TermVocabAttributes> typeOfModeOfCollectionsList,
            List<TermVocabAttributes> timeMethodList,
            Map<String, List<TermVocabAttributes>> typeOfSamplingProcedures,
            CMMStudyMapper.ParseResults<CMMStudyMapper.DataCollectionPeriod, DateTimeParseException> dataCollectionParseResult
    ) {
    }

    private record ArchiveResult(String dataAccess, HashMap<String, List<String>> dataAccessFreeTexts) {
    }

    private record CitationResult(
            String publicationYear,
            Map<String, String> titleStudy,
            Map<String, List<Creator>> creatorMap,
            Map<String, List<Pid>> pidMap,
            Map<String, Publisher> publisherMap
    ) {
    }
}
