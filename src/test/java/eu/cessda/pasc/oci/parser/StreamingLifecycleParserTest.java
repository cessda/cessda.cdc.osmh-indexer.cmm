package eu.cessda.pasc.oci.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import eu.cessda.pasc.oci.ResourceHandler;
import eu.cessda.pasc.oci.configurations.Repo;
import eu.cessda.pasc.oci.mock.data.ReposTestData;
import eu.cessda.pasc.oci.models.cmmstudy.*;
import eu.cessda.pasc.oci.models.cmmstudy.Creator;
import eu.cessda.pasc.oci.models.cmmstudy.Publisher;
import eu.cessda.pasc.oci.models.lifecycle.*;
import eu.cessda.pasc.oci.models.lifecycle.Universe;
import org.json.JSONException;
import org.junit.Test;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;

import static com.google.common.io.Files.getNameWithoutExtension;
import static eu.cessda.pasc.oci.parser.ParsingStrategies.parseDataAccessString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

public class StreamingLifecycleParserTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ParserTestUtilities utils = new ParserTestUtilities(objectMapper);

    @Test
    public void shouldReturnValidCMMStudyRecordFromAFullyFragmentRecord() throws IOException, XMLStreamException, ProcessingException, JSONException, URISyntaxException {
        // Given
        var expectedJson = ResourceHandler.getResourceAsString("json/synthetic_compliant_record_ddi_3_fragments.json");
        var repo = ReposTestData.getUKDSRepo();
        var recordXML = ResourceHandler.getResource("xml/ddi_3_3/compliant_fragments_cmm_ddi_3_3.xml");

        // When
        var xmlInputFactory = XMLInputFactory.newFactory();
        var source = new StreamSource(recordXML.openStream(), recordXML.toString());

        // Parse the document
        var parser = StreamingLifecycleParser.parseDocument(xmlInputFactory, source);

        /*
         * OAI-PMH Request
         */
        var uri = parser.getRequest().map(URI::create).orElse(null);

        /*
         * OAI-PMH Header
         */
        var recordHeader = parser.getRecordHeader();
        final String studyNumber;
        final String lastModified;
        if (recordHeader.isPresent()) {
            var h = recordHeader.get();
            studyNumber = h.identifier();
            lastModified = h.datestamp();
        } else {
            // Derive the study number from the file name
            studyNumber = getNameWithoutExtension(recordXML.toString());
            lastModified = OffsetDateTime.now(ZoneId.systemDefault()).toString();
        }

        var objectsByType = parser.getObjectsByType();

        // Should parse all supported types of objects
        assertThat(objectsByType).containsKeys(
                Organization.class,
                DataCollection.class,
                Methodology.class,
                StudyUnit.class,
                PhysicalInstance.class,
                Universe.class
        );

        // Get top level study unit, should only be 1
        var studyUnitList = objectsByType.get(StudyUnit.class);
        assertThat(studyUnitList).hasSize(1);

        var allComponents = parser.getObjectsById();

        // Get study unit
        var studyUnit = (StudyUnit) studyUnitList.getFirst();

        var cmmStudy = parseFragmentedStudy(repo, uri, studyNumber, lastModified, allComponents, "en", studyUnit);

        System.out.println(cmmStudy);

        utils.validateCMMStudyResultAgainstSchema(cmmStudy);

        String actualJson = objectMapper.writeValueAsString(cmmStudy);
        System.out.println(actualJson);

        // Check if the JSON generated differs from the expected source
        assertEquals(expectedJson, actualJson, true);

        System.out.println(cmmStudy);
    }

    private CMMStudy parseFragmentedStudy(Repo repository, URI uri, String studyNumber, String lastModified, Map<ObjectInformation, DDIObject> components, String defaultLang, StudyUnit studyUnit) throws URISyntaxException {


        /*
         * Abstract
         */
        var abstr = studyUnit.abstractMap();

        /*
         * Classifications
         */
        var classifications = new HashMap<String, List<TermVocabAttributes>>();
        studyUnit.coverage().topicalCoverage().subjects().forEach((lang, subjectList) -> {
            for (var subject : subjectList) {
                var termVocabAttributes = new TermVocabAttributes(subject.name(), subject.urn(), subject.id(), subject.content());
                if (lang == null || lang.equals("*")) {
                    lang = defaultLang;
                }
                classifications.computeIfAbsent(lang, k -> new ArrayList<>()).add(termVocabAttributes);
            }
        });

        /*
         * Citation related
         */
        var citation = studyUnit.citation();

        var creatorMap = new HashMap<String, List<Creator>>();
        var pidMap = new HashMap<String, List<Pid>>();
        var publisherMap = new HashMap<String, Publisher>();

        CMMStudyMapper.ParseResults<CMMStudyMapper.DataCollectionPeriod, DateTimeParseException> dataCollectionParseResult = XPaths.EMPTY_PARSE_RESULTS;
        String publicationYear = null;
        Map<String, String> titleStudy = Collections.emptyMap();

        if (citation != null) {


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
                        URI.create(researcherID.uri())
                );

                individualIdentification.individualName().fullName().forEach((lang, name) -> {
                    var crObj = new Creator(name, null, creatorIdentifier);
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
            switch (citation.publicationDate()) {
                case SimpleDateType simpleDateType -> {
                    publicationYear = simpleDateType.simpleDate();
                }
                case PeriodDateType periodDateType -> {
                    publicationYear = periodDateType.startDate();
                }
            }

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
                        names.names().forEach((lang, name)  -> {
                            // Get abbreviation, if exists
                            var abbr = names.abbreviations().get(lang);
                            var publisherObj = new Publisher(abbr, name);
                            publisherMap.put(lang, publisherObj);
                        });
                    } else if (ddiObject instanceof Individual individual) {
                        var names = individual.individualIdentification().individualName().fullName();
                        names.forEach((lang, name)  -> {
                            // Get abbreviation, if exists
                            var publisherObj = new Publisher(null, name);
                            publisherMap.put(lang, publisherObj);
                        });
                    }
                }
            }

            /*
             * Study Title
             */
            titleStudy = citation.title();
        }

        /*
         * Data access - includes free texts and URLs
         */
        String dataAccess = null;
        var dataAccessFreeTexts = new HashMap<String, List<String>>();
        for (var archiveRef : studyUnit.archiveReference()) {
            // Get referenced archive
            var objInf = archiveRef.objInf();
            DDIObject referencedObject = components.get(objInf);

            if (referencedObject instanceof Archive archive) {
                if (dataAccess == null) {
                    var accessTypeName = archive.access().accessTypeName();
                    for (var accessType : accessTypeName.values()) {
                        var parsedDataAccess = parseDataAccessString(accessType);
                        if (parsedDataAccess != null) {
                            dataAccess = parsedDataAccess;
                            break;
                        }
                    }
                }

                var accessDescription = archive.access().accessDescription();
                accessDescription.forEach((lang, description) -> {
                    dataAccessFreeTexts.computeIfAbsent(lang, k -> new ArrayList<>()).add(description);
                });
            }
        }

        /*
         * Data Access URL
         */
        var dataAccessUrl = Collections.<String, URI>emptyMap();

        /*
         * Type of Mode of Collections
         */
        var typeOfModeOfCollectionsList = new ArrayList<TermVocabAttributes>();

        /*
         * Data Collection
         */
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
                    {
                        var event = collectionEvent.getFirst();
                        var collectionDate = event.collectionDate();

                        try {
                            switch (collectionDate) {
                                case SimpleDateType simpleDateType -> {
                                    var simpleDate = simpleDateType.simpleDate();
                                    year = ParsingStrategies.parseDateIntoYear(simpleDate);
                                }
                                case PeriodDateType periodDateType -> {
                                    startDate = periodDateType.startDate();
                                    endDate = periodDateType.endDate();
                                    year = ParsingStrategies.parseDateIntoYear(startDate);
                                }
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
                            var term = new TermVocabAttributes(cv.name(), cv.urn(), cv.id(), cv.content());
                            typeOfModeOfCollectionsList.add(term);
                        }
                    }
                }
            }
        }

        var typeOfModeOfCollections = Map.<String, List<TermVocabAttributes>>of(defaultLang, typeOfModeOfCollectionsList);

        /*
         * Data Kind
         */
        var dataKindFreeTextList = new ArrayList<DataKindFreeText>();
        for (var kindOfData : studyUnit.kindOfData()) {
            var dataKind = new DataKindFreeText(kindOfData.controlledVocabulary().content(), kindOfData.type());
            dataKindFreeTextList.add(dataKind);
        }
        var dataKindFreeTexts = Map.<String, List<DataKindFreeText>>of(defaultLang, dataKindFreeTextList);

        /*
         * File Languages
         */
        var fileLanguages = new HashSet<String>();
        for (var phyInstRef : studyUnit.physicalInstanceReference()) {
            // Get referenced PhysicalInstance
            var objInf = phyInstRef.objInf();
            var referencedObject = components.get(objInf);

            if (referencedObject instanceof PhysicalInstance physicalInstance) {
                // Get filenames from citation
                var physicalInstanceCitation = physicalInstance.citation();
                if (physicalInstanceCitation != null) {
                    // fileLanguages.addAll(citation.language());
                    fileLanguages.addAll(physicalInstanceCitation.title().keySet());
                }
            }
        }

        /*
         * Funding
         */
        var funding = new HashMap<String, List<Funding>>();
        for (var fundingInformation : studyUnit.fundingInformation()) {
            String grantNumber = fundingInformation.grantNumber();

            var orgRef = fundingInformation.agencyOrganizationReference();

            var referencedObject = components.get(orgRef.objInf());
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

        /*
         * General Data Formats
         */
        var generalDataFormats = Collections.<String, List<TermVocabAttributes>>emptyMap();

        /*
         * Keywords
         */
        var keywords = Collections.<String, List<TermVocabAttributes>>emptyMap();

        /*
         * Related Publications
         */
        var relatedPublications = Collections.<String, List<RelatedPublication>>emptyMap();
        // TODO implement
        //studyUnit.otherMaterial()

        /*
         * Sampling Procedure
         */
        var samplingProcedureFreeTexts = Collections.<String, List<String>>emptyMap();

        /*
         * Series
         */
        var series = Collections.<String, List<Series>>emptyMap();

        /*
         * Study Area Countries
         */
        var studyAreaCountries = Collections.<String, List<Country>>emptyMap();

        /*
         * Study URL
         */
        var studyUrl = Collections.<String, URI>emptyMap();

        /*
         * Type of Time Methods
         */
        var typeOfTimeMethods = Collections.<String, List<TermVocabAttributes>>emptyMap();

        /*
         * Type of Sampling Procedures
         */
        var typeOfSamplingProcedures = Collections.<String, List<VocabAttributes>>emptyMap();

        /*
         * Unit Types
         */
        var unitTypes = new ArrayList<TermVocabAttributes>();
        for (var analysisUnit : studyUnit.analysisUnit()) {
            var vocab = analysisUnit.name();
            var vocabUri = analysisUnit.urn();
            var id = analysisUnit.id();
            var term = analysisUnit.content();
            var type = new TermVocabAttributes(vocab, vocabUri, id, term);
            unitTypes.add(type);
        }
        var unitTypeMap = Map.<String, List<TermVocabAttributes>>of(defaultLang, unitTypes);

        /*
         * Universe
         */
        // TODO implement
        var universe = Collections.<String, eu.cessda.pasc.oci.models.cmmstudy.Universe>emptyMap();
        //studyUnit.conceptualComponent().universeScheme().universe()

        // Extract data collection parse results
        var dataCollectionResults = dataCollectionParseResult.results();

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
                samplingProcedureFreeTexts,
                series,
                studyAreaCountries,
                studyNumber,
                studyUrl,
                typeOfModeOfCollections,
                titleStudy,
                typeOfTimeMethods,
                typeOfSamplingProcedures,
                unitTypeMap,
                universe,
                lastModified,
                OaiPmhHelpers.buildGetStudyFullUrl(repository.url(), studyNumber, repository.preferredMetadataParam()),
                uri
        );
    }
}
