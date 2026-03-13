package eu.cessda.pasc.oci.parser;

import eu.cessda.pasc.oci.models.lifecycle.*;
import lombok.extern.slf4j.Slf4j;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import java.util.*;
import java.util.regex.Pattern;

import static javax.xml.stream.XMLStreamConstants.*;

@Slf4j
public class StreamingLifecycleParser {

    // Namespaces
    private static final String DDI_ARCHIVE = "ddi:archive:3_3";
    private static final String DDI_CONCEPTUALCOMPONENT = "ddi:conceptualcomponent:3_3";
    private static final String DDI_DATACOLLECTION = "ddi:datacollection:3_3";
    private static final String DDI_INSTANCE = "ddi:instance:3_3";
    private static final String DDI_PHYSICALINSTANCE = "ddi:physicalinstance:3_3";
    private static final String DDI_REUSABLE = "ddi:reusable:3_3";
    private static final String DDI_STUDYUNIT = "ddi:studyunit:3_3";

    // QNames
    private static final QName FRAGMENT = new QName(DDI_INSTANCE, "Fragment");
    private static final QName FRAGMENT_INSTANCE = new QName(DDI_INSTANCE, "FragmentInstance");
    private static final QName TOP_LEVEL_REFERENCE = new QName(DDI_INSTANCE, "TopLevelReference");

    private static final QName ORGANIZATION = new QName(DDI_ARCHIVE, "Organization");
    private static final QName ORGANIZATION_NAME = new QName(DDI_ARCHIVE, "OrganizationName");
    private static final QName ORGANIZATION_IDENTIFICATION = new QName(DDI_ARCHIVE, "OrganizationIdentification");

    private static final QName UNIVERSE = new QName(DDI_CONCEPTUALCOMPONENT, "Universe");
    private static final QName UNIVERSE_NAME = new QName(DDI_CONCEPTUALCOMPONENT, "UniverseName");

    private static final QName COLLECTION_EVENT = new QName(DDI_DATACOLLECTION, "CollectionEvent");
    private static final QName DATA_COLLECTION = new QName(DDI_DATACOLLECTION, "DataCollection");
    private static final QName DATA_COLLECTION_DATE = new QName(DDI_DATACOLLECTION, "DataCollectionDate");
    private static final QName METHODOLOGY = new QName(DDI_DATACOLLECTION, "Methodology");
    private static final QName METHODOLOGY_REFERENCE = new QName(DDI_DATACOLLECTION, "MethodologyReference");
    private static final QName MODE_OF_COLLECTION = new QName(DDI_DATACOLLECTION, "ModeOfCollection");
    private static final QName SAMPLING_PROCEDURE = new QName(DDI_DATACOLLECTION, "SamplingProcedure");
    private static final QName TIME_METHOD = new QName(DDI_DATACOLLECTION, "TimeMethod");
    private static final QName TYPE_OF_SAMPLING_PROCEDURE = new QName(DDI_DATACOLLECTION, "TypeOfSamplingProcedure");
    private static final QName TYPE_OF_TIME_METHOD = new QName(DDI_DATACOLLECTION, "TypeOfTimeMethod");

    private static final QName DATA_FILE_IDENTIFICATION = new QName(DDI_PHYSICALINSTANCE, "DataFileIdentification");
    private static final QName DATA_FILE_URI = new QName(DDI_PHYSICALINSTANCE, "DataFileURI");
    private static final QName PHYSICAL_INSTANCE = new QName(DDI_PHYSICALINSTANCE, "PhysicalInstance");

    private static final QName ABSTRACT = new QName(DDI_REUSABLE, "Abstract");
    private static final QName AGENCY_ORGANIZATION_REFERENCE = new QName(DDI_REUSABLE, "AgencyOrganizationReference");
    private static final QName ANALYSIS_UNIT = new QName(DDI_REUSABLE, "AnalysisUnit");
    private static final QName ARCHIVE_REFERENCE = new QName(DDI_REUSABLE, "ArchiveReference");
    private static final QName CITATION = new QName(DDI_REUSABLE, "Citation");
    private static final QName CONTENT = new QName(DDI_REUSABLE, "Content");
    private static final QName CONTRIBUTOR = new QName(DDI_REUSABLE, "Contributor");
    private static final QName CONTRIBUTOR_NAME = new QName(DDI_REUSABLE, "ContributorName");
    private static final QName CONTRIBUTOR_REFERENCE = new QName(DDI_REUSABLE, "ContributorReference");
    private static final QName CONTRIBUTOR_ROLE = new QName(DDI_REUSABLE, "ContributorRole");
    private static final QName COVERAGE = new QName(DDI_REUSABLE, "Coverage");
    private static final QName COUNTRY_CODE = new QName(DDI_REUSABLE, "CountryCode");
    private static final QName CREATOR = new QName(DDI_REUSABLE, "Creator");
    private static final QName CREATOR_NAME = new QName(DDI_REUSABLE, "CreatorName");
    private static final QName CREATOR_REFERENCE = new QName(DDI_REUSABLE, "CreatorReference");
    private static final QName DATA_COLLECTION_REFERENCE = new QName(DDI_REUSABLE, "DataCollectionReference");
    private static final QName DESCRIPTION = new QName(DDI_REUSABLE, "Description");
    private static final QName FUNDING_INFORMATION = new QName(DDI_REUSABLE, "FundingInformation");
    private static final QName GRANT_NUMBER = new QName(DDI_REUSABLE, "GrantNumber");
    private static final QName KEYWORD = new QName(DDI_REUSABLE, "Keyword");
    private static final QName KIND_OF_DATA = new QName(DDI_REUSABLE, "KindOfData");
    private static final QName IDENTIFIER_CONTENT = new QName(DDI_REUSABLE, "IdentifierContent");
    private static final QName INTERNATIONAL_IDENTIFIER = new QName(DDI_REUSABLE, "InternationalIdentifier");
    private static final QName LABEL = new QName(DDI_REUSABLE, "Label");
    private static final QName MANAGING_AGENCY = new QName(DDI_REUSABLE, "ManagingAgency");
    private static final QName PHYSICAL_INSTANCE_REFERENCE = new QName(DDI_REUSABLE, "PhysicalInstanceReference");
    private static final QName PUBLICATION_DATE = new QName(DDI_REUSABLE, "PublicationDate");
    private static final QName PUBLISHER = new QName(DDI_REUSABLE, "Publisher");
    private static final QName PUBLISHER_NAME = new QName(DDI_REUSABLE, "PublisherName");
    private static final QName PUBLISHER_REFERENCE = new QName(DDI_REUSABLE, "PublisherReference");
    private static final QName REFERENCE_DATE = new QName(DDI_REUSABLE, "ReferenceDate");
    private static final QName SPATIAL_COVERAGE = new QName(DDI_REUSABLE, "SpatialCoverage");
    private static final QName STRING = new QName(DDI_REUSABLE, "String");
    private static final QName SUBJECT = new QName(DDI_REUSABLE, "Subject");
    private static final QName TEMPORAL_COVERAGE = new QName(DDI_REUSABLE, "TemporalCoverage");
    private static final QName TITLE = new QName(DDI_REUSABLE, "Title");
    private static final QName TOPICAL_COVERAGE = new QName(DDI_REUSABLE, "TopicalCoverage");
    private static final QName UNIVERSE_REFERENCE = new QName(DDI_REUSABLE, "UniverseReference");

    private static final QName STUDY_UNIT = new QName(DDI_STUDYUNIT, "StudyUnit");

    private static final Set<QName> LOCAL_NAMES = Set.of(
            new QName(DDI_REUSABLE, "URN"),
            new QName(DDI_REUSABLE, "Agency"),
            new QName(DDI_REUSABLE, "ID"),
            new QName(DDI_REUSABLE, "Version")
    );

    // URN Matcher
    private static final Pattern DDI_URN_REGEX = Pattern.compile("^urn:ddi:([^:]*):([^:]*):([^:]*)$");

    private final XMLInputFactory factory;
    private final Source source;
    private final ArrayList<DDIObject> parsedObjects = new ArrayList<>();

    private TrackingXMLReader reader;

    public StreamingLifecycleParser(XMLInputFactory factory, Source source) {
        this.factory = factory;
        this.source = source;
    }

    public Map<Class<? extends DDIObject>, List<DDIObject>> getObjectsByType() {
        var objectMap = new HashMap<Class<? extends DDIObject>, List<DDIObject>>();
        for (var object : parsedObjects) {
            objectMap.computeIfAbsent(object.getClass(), k -> new ArrayList<>()).add(object);
        }
        return objectMap;
    }

    public Map<ObjectInformation, DDIObject> getObjectsById() {
        var objectMap = new HashMap<ObjectInformation, DDIObject>();
        for (var object : parsedObjects) {
            objectMap.put(object.objInf(), object);
        }
        return objectMap;
    }

    public static StreamingLifecycleParser parseDocument(XMLInputFactory factory, Source source) throws XMLStreamException {
        var parser = new StreamingLifecycleParser(factory, source);

        // Parse the document
        parser.parseDDI();

        // Return the parsed document
        return parser;
    }

    private void parseDDI() throws XMLStreamException {

        // Create reader
        var streamReader = factory.createXMLStreamReader(source);
        reader = new TrackingXMLReader(streamReader);

        boolean found = false;

        try {
            // Get the root DDI element
            do {
                if (reader.getEventType() == START_ELEMENT && reader.getName().equals(FRAGMENT_INSTANCE)) {
                    // Start parsing
                    parseFromRoot();
                    found = true;
                }
            } while (reader.next() != END_DOCUMENT);
        } finally {
            reader = null;
        }

        if (!found) {
            throw new XMLStreamException("Expected element \"" + FRAGMENT_INSTANCE + "\" not found");
        }
    }

    private void parseFromRoot() throws XMLStreamException {
        int initialDepth = reader.getDepth();
        do {
            if (reader.getEventType() == START_ELEMENT) {
                // Switch on element name
                if (reader.getName().equals(TOP_LEVEL_REFERENCE)) {
                    var topLevel = parseReference();
                    parsedObjects.add(topLevel);
                } else if (reader.getName().equals(FRAGMENT)) {
                    var ddiObject = parseObject();
                    if (ddiObject != null) {
                        parsedObjects.add(ddiObject);
                    }
                }
            }
        } while (reader.getDepth() >= initialDepth && reader.next() != END_DOCUMENT);

        log.warn(parsedObjects.toString());
    }

    // Parses the object contained within the DDI fragment
    private DDIObject parseObject() throws XMLStreamException {
        // Check document position
        validateElement(FRAGMENT);

        var fragmentElement = reader.nextElement();

        if (fragmentElement.equals(ORGANIZATION)) {
            return parseOrganization();
        } else if (fragmentElement.equals(DATA_COLLECTION)) {
            return parseDataCollection();
        } else if (fragmentElement.equals(METHODOLOGY)) {
            return parseMethodology();
        } else if (fragmentElement.equals(STUDY_UNIT)) {
            return parseStudyUnit();
        } else if (fragmentElement.equals(PHYSICAL_INSTANCE)) {
            return parsePhysicalInstance();
        } else if (fragmentElement.equals(UNIVERSE)) {
            return parseUniverse();
        }

        return null;
    }

    private Universe parseUniverse() throws XMLStreamException {
        validateElement(UNIVERSE);

        // Stream to the next element
        reader.nextTag();

        // Parse object information
        var objInf = parseObjectInformation();

        Map<String, String> universeName = Collections.emptyMap();
        Map<String, String> label = Collections.emptyMap();

        do {
            if (reader.getEventType() == START_ELEMENT) {
                var qName = reader.getName();
                if (qName.equals(UNIVERSE_NAME)) {
                    reader.nextTag();
                    universeName = extractMultilingualStrings();
                } else if (qName.equals(LABEL)) {
                    reader.nextTag();
                    label = extractMultilingualContent();
                }
            }
        } while (reader.next() != END_ELEMENT || !reader.getName().equals(UNIVERSE));

        return new Universe(objInf, universeName, label);
    }

    private PhysicalInstance parsePhysicalInstance() throws XMLStreamException {
        validateElement(PHYSICAL_INSTANCE);

        // Stream to the next element
        reader.nextTag();

        // Parse object information
        var objInf = parseObjectInformation();

        Citation citation = null;
        List<String> dataFileUris = new ArrayList<>();

        do {
            if (reader.getEventType() == START_ELEMENT) {
                var qName = reader.getName();
                if (qName.equals(CITATION)) {
                    citation = parseCitation();
                } else if (qName.equals(DATA_COLLECTION_REFERENCE)) {
                    var dataFileUri = parseDataFileIdentification();
                    dataFileUris.add(dataFileUri);
                }
            }
        } while (reader.next() != END_ELEMENT || !reader.getName().equals(PHYSICAL_INSTANCE));

        return new PhysicalInstance(objInf, citation, dataFileUris);
    }

    private String parseDataFileIdentification() throws XMLStreamException {
        validateElement(DATA_FILE_IDENTIFICATION);

        // Stream to the next element
        reader.nextTag();

        do {
            if (reader.getEventType() == START_ELEMENT && reader.getName().equals(DATA_FILE_URI)) {
                return reader.getElementText();
            }
        } while (reader.next() != END_ELEMENT || !reader.getName().equals(DATA_FILE_IDENTIFICATION));

        throw new XMLStreamException("Expected element \"" + DATA_FILE_URI + "\" not found", reader.getLocation());
    }

    private StudyUnit parseStudyUnit() throws XMLStreamException {
        validateElement(STUDY_UNIT);

        // Stream to the next element
        reader.nextTag();

        // Parse object information
        var objInf = parseObjectInformation();

        Citation citation = null;
        Map<String, String> abstractMap = Collections.emptyMap();
        Reference universeReference = null;
        List<FundingInformation> fundingInformationList = new ArrayList<>();
        Coverage coverage = null;
        List<ControlledVocabulary> analysisUnitList = new ArrayList<>();
        List<ControlledVocabulary> kindOfDataList = new ArrayList<>();
        List<Reference> dataCollectionReferenceList = new ArrayList<>();
        List<Reference> physicalInstanceReferenceList = new ArrayList<>();
        List<Reference> archiveReferenceList = new ArrayList<>();

        do {
            if (reader.getEventType() == START_ELEMENT) {
                var qName = reader.getName();
                if (qName.equals(CITATION)) {
                    citation = parseCitation();
                } else if (qName.equals(ABSTRACT)) {
                    reader.nextTag();
                    abstractMap = extractMultilingualContent();
                } else if (qName.equals(UNIVERSE_REFERENCE)) {
                    universeReference = parseReference();
                } else if (qName.equals(FUNDING_INFORMATION)) {
                    var fundingInformation = parseFundingInformation();
                    fundingInformationList.add(fundingInformation);
                } else if (qName.equals(COVERAGE)) {
                    coverage = parseCoverage();
                } else if (qName.equals(ANALYSIS_UNIT)) {
                    var analysisUnit = parseControlledVocabularyInformation();
                    analysisUnitList.add(analysisUnit);
                } else if (qName.equals(KIND_OF_DATA)) {
                    var kindOfData = parseControlledVocabularyInformation();
                    kindOfDataList.add(kindOfData);
                } else if (qName.equals(DATA_COLLECTION_REFERENCE)) {
                    var dataCollectionReference = parseReference();
                    dataCollectionReferenceList.add(dataCollectionReference);
                } else if (qName.equals(PHYSICAL_INSTANCE_REFERENCE)) {
                    var physicalInstRef = parseReference();
                    physicalInstanceReferenceList.add(physicalInstRef);
                } else if (qName.equals(ARCHIVE_REFERENCE)) {
                    var archiveRef = parseReference();
                    archiveReferenceList.add(archiveRef);
                }
            }

        } while (reader.next() != END_ELEMENT || !reader.getName().equals(STUDY_UNIT));

        return new StudyUnit(
            objInf,
            citation,
            abstractMap,
            universeReference,
            fundingInformationList,
            coverage,
            analysisUnitList,
            kindOfDataList,
            dataCollectionReferenceList,
            physicalInstanceReferenceList,
            archiveReferenceList
        );
    }

    private Coverage parseCoverage() throws XMLStreamException {
        validateElement(COVERAGE);

        reader.nextTag();

        TopicalCoverage topicalCoverage = null;
        SpatialCoverage spatialCoverage = null;
        TemporalCoverage temporalCoverage = null;

        do {
            if (reader.getEventType() == START_ELEMENT) {
                var qName = reader.getName();
                if (qName.equals(TOPICAL_COVERAGE)) {
                    topicalCoverage = parseTopicalCoverage();
                } else if (qName.equals(SPATIAL_COVERAGE)) {
                    spatialCoverage = parseSpatialCoverage();
                } else if (qName.equals(TEMPORAL_COVERAGE)) {
                    temporalCoverage = parseTemporalCoverage();
                }
            }
        } while (reader.next() != END_ELEMENT || !reader.getName().equals(COVERAGE));

        return new Coverage(topicalCoverage, spatialCoverage, temporalCoverage);
    }

    private TemporalCoverage parseTemporalCoverage() throws XMLStreamException {
        validateElement(TEMPORAL_COVERAGE);

        reader.nextTag();

        // Parse object information
        var objInf = parseObjectInformation();
        DateType referenceDate = null;

        do {
            if (reader.getEventType() == START_ELEMENT && reader.getName().equals(REFERENCE_DATE)) {
                referenceDate = parseReferenceDate();
            }
        } while (reader.next() != END_ELEMENT || !reader.getName().equals(TEMPORAL_COVERAGE));

        return new TemporalCoverage(objInf, referenceDate);
    }

    private DateType parseReferenceDate() throws XMLStreamException {
        return parseDateType(REFERENCE_DATE);
    }

    private SpatialCoverage parseSpatialCoverage() throws XMLStreamException {
        validateElement(SPATIAL_COVERAGE);

        reader.nextTag();

        // Parse object information
        var objectInformation = parseObjectInformation();
        Map<String, String> description = Collections.emptyMap();
        var countryCodes = new ArrayList<String>();

        do {
            if (reader.getEventType() == START_ELEMENT) {
                var qName = reader.getName();
                if (qName.equals(DESCRIPTION)) {
                    reader.nextTag();
                    description = extractMultilingualContent();
                } else if (qName.equals(COUNTRY_CODE)) {
                    var code = reader.getElementText();
                    countryCodes.add(code);
                }
            }
        } while (reader.next() != END_ELEMENT || !reader.getName().equals(SPATIAL_COVERAGE));

        return new SpatialCoverage(objectInformation, description, countryCodes);
    }

    private TopicalCoverage parseTopicalCoverage() throws XMLStreamException {
        validateElement(TOPICAL_COVERAGE);

        reader.nextTag();

        // Parse object information
        var objectInformation = parseObjectInformation();

        // Parse subjects
        var subjects = new ArrayList<ControlledVocabulary>();
        var keywords = new ArrayList<ControlledVocabulary>();

        do {
            if (reader.getEventType() == START_ELEMENT) {
                var qName = reader.getName();
                if (qName.equals(SUBJECT)) {
                    var subject = parseControlledVocabularyInformation();
                    subjects.add(subject);
                } else if (qName.equals(KEYWORD)) {
                    var keyword = parseControlledVocabularyInformation();
                    keywords.add(keyword);
                }
            }
        } while (reader.next() != END_ELEMENT || !reader.getName().equals(TOPICAL_COVERAGE));

        return new TopicalCoverage(objectInformation, subjects, keywords);
    }

    private FundingInformation parseFundingInformation() throws XMLStreamException {
        validateElement(FUNDING_INFORMATION);

        reader.nextTag();

        Reference agencyOrganizationReference = null;
        String grantNumber = null;

        do {
            if (reader.getEventType() == START_ELEMENT) {
                if (reader.getName().equals(AGENCY_ORGANIZATION_REFERENCE)) {
                    agencyOrganizationReference = parseReference();
                } else if (reader.getName().equals(GRANT_NUMBER)) {
                    grantNumber = reader.getElementText();
                }
            }
        } while (reader.next() != END_ELEMENT || !reader.getName().equals(FUNDING_INFORMATION));

        return new FundingInformation(agencyOrganizationReference, grantNumber);
    }

    private Citation parseCitation() throws XMLStreamException {
        validateElement(CITATION);

        Map<String, String> title = Collections.emptyMap();
        Creator creator = null;
        Publisher publisher = null;
        List<Contributor> contributors = new ArrayList<>();
        DateType publicationDate = null;
        InternationalIdentifier internationalIdentifier = null;

        do {
            if (reader.getEventType() == START_ELEMENT) {
                var qName = reader.getName();
                if (qName.equals(TITLE)) {
                    reader.nextTag();
                    title = extractMultilingualStrings();
                } else if (qName.equals(CREATOR)) {
                    creator = parseCreator();
                } else if (qName.equals(PUBLISHER)) {
                    publisher = parsePublisher();
                } else if (qName.equals(CONTRIBUTOR)) {
                    var contributor = parseContributor();
                    contributors.add(contributor);
                } else if (qName.equals(PUBLICATION_DATE)) {
                    publicationDate = parsePublicationDate();
                } else if (qName.equals(INTERNATIONAL_IDENTIFIER)) {
                    internationalIdentifier = parseInternationalIdentifier();
                }
            }

        } while (reader.next() != END_ELEMENT || !reader.getName().equals(CITATION));

        return new Citation(title, creator, publisher, contributors, publicationDate, internationalIdentifier);
    }

    private InternationalIdentifier parseInternationalIdentifier() throws XMLStreamException {
        validateElement(INTERNATIONAL_IDENTIFIER);

        reader.nextTag();

        String identifierContent = null;
        String managingAgency = null;

        do {
            if (reader.getEventType() == START_ELEMENT) {
                if (reader.getName().equals(IDENTIFIER_CONTENT)) {
                    identifierContent = reader.getElementText();
                } else if (reader.getName().equals(MANAGING_AGENCY)) {
                    managingAgency = reader.getElementText();
                }
            }
        } while (reader.next() != END_ELEMENT || !reader.getName().equals(INTERNATIONAL_IDENTIFIER));

        return new InternationalIdentifier(identifierContent, managingAgency);
    }

    private DateType parsePublicationDate() throws XMLStreamException {
        return parseDateType(PUBLICATION_DATE);
    }

    private Contributor parseContributor() throws XMLStreamException {
        validateElement(CONTRIBUTOR);

        reader.nextTag();

        // Contributor
        Map<String, String> contributorName = Collections.emptyMap();
        ControlledVocabulary contributorRole = null;
        Reference contributorReference = null;

        do {
            if (reader.getEventType() == START_ELEMENT) {
                var qName = reader.getName();
                if (qName.equals(CONTRIBUTOR_NAME)) {
                    // Stream to the next element
                    reader.nextTag();

                    // Extract creator names
                    contributorName = extractMultilingualStrings();
                } else if (qName.equals(CONTRIBUTOR_ROLE)) {
                    contributorRole = parseControlledVocabularyInformation();
                } else if (qName.equals(CONTRIBUTOR_REFERENCE)) {
                    // Extract creator reference
                    contributorReference = parseReference();
                }
            }

        } while (reader.next() != END_ELEMENT || !reader.getName().equals(CONTRIBUTOR));

        return new Contributor(contributorReference, contributorRole, contributorName);
    }

    private Publisher parsePublisher() throws XMLStreamException {
        validateElement(PUBLISHER);

        reader.nextTag();

        // Publisher
        Map<String, String> publisherName = Collections.emptyMap();
        Reference publisherReference = null;

        do {
            if (reader.getEventType() == START_ELEMENT) {
                var qName = reader.getName();
                if (qName.equals(PUBLISHER_NAME)) {
                    // Stream to the next element
                    reader.nextTag();

                    // Extract creator names
                    publisherName = extractMultilingualStrings();
                } else if (qName.equals(PUBLISHER_REFERENCE)) {
                    // Extract creator reference
                    publisherReference = parseReference();
                }
            }

        } while (reader.next() != END_ELEMENT || !reader.getName().equals(PUBLISHER));

        return new Publisher(publisherReference, publisherName);
    }

    private Creator parseCreator() throws XMLStreamException {
        validateElement(CREATOR);

        reader.nextTag();

        // Creator
        Map<String, String> creatorName = Collections.emptyMap();
        Reference creatorReference = null;

        do {
            if (reader.getEventType() == START_ELEMENT) {
                var qName = reader.getName();
                if (qName.equals(CREATOR_NAME)) {
                    // Stream to the next element
                    reader.nextTag();

                    // Extract creator names
                    creatorName = extractMultilingualStrings();
                } else if (qName.equals(CREATOR_REFERENCE)) {
                    // Extract creator reference
                    creatorReference = parseReference();
                }
            }

        } while (reader.next() != END_ELEMENT || !reader.getName().equals(CREATOR));

        return new Creator(creatorReference, creatorName);
    }

    private Methodology parseMethodology() throws XMLStreamException {
        validateElement(METHODOLOGY);

        // Stream to the next element
        reader.nextTag();

        // Parse object information
        var objInf = parseObjectInformation();

        SamplingProcedure samplingProcedure = null;
        TimeMethod timeMethod = null;

        do {
            if (reader.getEventType() == START_ELEMENT) {
                var qName = reader.getName();
                if (qName.equals(SAMPLING_PROCEDURE)) {
                    samplingProcedure = parseSamplingProcedure();
                } else if (qName.equals(TIME_METHOD)) {
                    timeMethod = parseTimeMethod();
                }
            }
        } while (reader.next() != END_ELEMENT || !reader.getName().equals(METHODOLOGY));

        return new Methodology(objInf, samplingProcedure, timeMethod);
    }

    private TimeMethod parseTimeMethod() throws XMLStreamException {
        validateElement(TIME_METHOD);

        // Stream to the next element
        reader.nextTag();

        var objInf = parseObjectInformation();

        int depth = reader.getDepth();

        ControlledVocabulary controlledVocabulary = null;
        String typeOfTimeMethod = null;

        // Current event
        int event = reader.getEventType();
        do {
            if (event == START_ELEMENT) {
                var elementName = reader.getName();
                if (elementName.equals(TYPE_OF_TIME_METHOD)) {
                    // Parse controlled vocabulary information
                    controlledVocabulary = parseControlledVocabularyInformation();

                    // Get mode of collection
                    typeOfTimeMethod = reader.getElementText();
                }
            }

            // Get next event
            event = reader.next();
        } while (depth <= reader.getDepth());

        return new TimeMethod(objInf, controlledVocabulary, typeOfTimeMethod);
    }

    private SamplingProcedure parseSamplingProcedure() throws XMLStreamException {
        validateElement(SAMPLING_PROCEDURE);

        // Stream to the next element
        reader.nextTag();

        var objInf = parseObjectInformation();

        int depth = reader.getDepth();

        ControlledVocabulary controlledVocabulary = null;
        String typeOfSamplingProcedure = null;

        Map<String, String> content = Collections.emptyMap();

        // Current event
        int event = reader.getEventType();
        do {
            if (event == START_ELEMENT) {
                var qName = reader.getName();
                if (qName.equals(TYPE_OF_SAMPLING_PROCEDURE)) {
                    // Parse controlled vocabulary information
                    controlledVocabulary = parseControlledVocabularyInformation();

                    // Get mode of collection
                    typeOfSamplingProcedure = reader.getElementText();
                } else if (qName.equals(DESCRIPTION)) {
                    // Parse content
                    int d2 = reader.getDepth();
                    do {
                        if (reader.next() == START_ELEMENT && reader.getName().equals(CONTENT)) {
                            content = extractMultilingualContent();
                        }
                    } while (d2 <= reader.getDepth());
                }
            }

            // Get next event
            event = reader.next();
        } while (depth <= reader.getDepth());

        return new SamplingProcedure(objInf, controlledVocabulary, typeOfSamplingProcedure, content);
    }

    private DataCollection parseDataCollection() throws XMLStreamException {
        validateElement(DATA_COLLECTION);

        // Stream to the next element
        reader.nextTag();

        // Parse object information
        var objInf = parseObjectInformation();

        Reference methodologyReference = null;
        CollectionEvent collectionEvent = null;

        do {
            if (reader.getEventType() == START_ELEMENT) {
                if (reader.getName().equals(COLLECTION_EVENT)) {
                    collectionEvent = parseCollectionEvent();
                } else if (reader.getName().equals(METHODOLOGY_REFERENCE)) {
                    // Parse object information
                    methodologyReference = parseReference();
                }
            }
        } while (reader.next() != END_ELEMENT || !reader.getName().equals(DATA_COLLECTION));

        return new DataCollection(objInf, collectionEvent, methodologyReference);
    }

    private CollectionEvent parseCollectionEvent() throws XMLStreamException {
        // Stream to the next element
        reader.nextTag();

        // Parse object information
        var collectionEventObjInf = parseObjectInformation();

        DateType collectionDates = null;
        var modeOfCollections = new ArrayList<ModeOfCollection>();

        // Current event
        int event = reader.getEventType();
        int depth = reader.getDepth() - 1;
        do {
            // Get next event
            if (event == START_ELEMENT) {
                if (reader.getName().equals(DATA_COLLECTION_DATE)) {
                    // Parse date
                    collectionDates = parseDataCollectionDate();
                } else if (reader.getName().equals(MODE_OF_COLLECTION)) {
                    // Parse mode of collection
                    var modeOfCollection = parseModeOfCollection();
                    modeOfCollections.add(modeOfCollection);
                }
            }
            // Get next event
            event = reader.next();
        } while (depth <= reader.getDepth());

        return new CollectionEvent(collectionEventObjInf, collectionDates, modeOfCollections);
    }

    private DateType parseDataCollectionDate() throws XMLStreamException {
        return parseDateType(DATA_COLLECTION_DATE);
    }

    private DateType parseDateType(QName element) throws XMLStreamException {
        validateElement(element);

        int depth = reader.getDepth();

        String simpleDate = null;
        String startDate = null;
        String endDate = null;

        // Current event
        int event = reader.getEventType();
        do {
            if (event == START_ELEMENT) {
                var elementName = reader.getName();
                if (elementName.equals(new QName(DDI_REUSABLE, "SimpleDate"))) {
                    simpleDate = reader.getElementText();
                } else if (elementName.equals(new QName(DDI_REUSABLE, "StartDate"))) {
                    startDate = reader.getElementText();
                } else if (elementName.equals(new QName(DDI_REUSABLE, "EndDate"))) {
                    endDate = reader.getElementText();
                }
            }

            // Get next event
            event = reader.next();
        } while (depth <= reader.getDepth());

        if (simpleDate != null) {
            if (startDate != null || endDate != null) {
                throw new XMLStreamException("DateType cannot have SimpleDate and either StartDate or EndDate", reader.getLocation());
            }
            return new SimpleDateType(simpleDate);
        } else {
            if (startDate != null || endDate != null) {
                return new PeriodDateType(startDate, endDate);
            }
            throw new XMLStreamException("DateType has no dates", reader.getLocation());
        }
    }

    private ModeOfCollection parseModeOfCollection() throws XMLStreamException {
        validateElement(MODE_OF_COLLECTION);

        // Stream to next element
        reader.nextTag();

        // Parse information
        var objInf = parseObjectInformation();

        int depth = reader.getDepth();

        ControlledVocabulary controlledVocabulary = null;
        String modeOfCollection = null;

        // Current event
        int event = reader.getEventType();
        do {
            if (event == START_ELEMENT) {
                var elementName = reader.getName();
                if (elementName.equals(new QName(DDI_DATACOLLECTION, "TypeOfModeOfCollection"))) {
                    // Parse controlled vocabulary information
                    controlledVocabulary = parseControlledVocabularyInformation();

                    // Get mode of collection
                    modeOfCollection = reader.getElementText();
                }
            }

            // Get next event
            event = reader.next();
        } while (depth <= reader.getDepth());

        return new ModeOfCollection(objInf, controlledVocabulary, modeOfCollection);
    }

    private ControlledVocabulary parseControlledVocabularyInformation() {
        // Parse attributes
        String id = null;
        String agencyName = null;
        String versionId = null;

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            switch (reader.getAttributeLocalName(i)) {
                case "controlledVocabularyID" -> id = reader.getAttributeValue(i);
                case "controlledVocabularyAgencyName" -> agencyName = reader.getAttributeValue(i);
                case "controlledVocabularyVersionID" -> versionId = reader.getAttributeValue(i);
            }
        }

        return new ControlledVocabulary(id, agencyName, versionId);
    }

    private Organization parseOrganization() throws XMLStreamException {
        validateElement(ORGANIZATION);

        // Stream to next element
        reader.nextTag();

        // Parse object information
        var objInf = parseObjectInformation();

        Map<String, String> names = Collections.emptyMap();

        do {
            if (reader.getEventType() == START_ELEMENT && reader.getName().equals(ORGANIZATION_IDENTIFICATION)) {
                int depth = reader.getDepth();
                do {
                    // Get next event
                    if (reader.next() == START_ELEMENT && reader.getName().equals(ORGANIZATION_NAME)) {
                        // Parse OrganizationName
                        names = parseOrganizationName();
                    }
                } while (depth <= reader.getDepth());
            }
        } while (reader.next() != END_ELEMENT || !reader.getName().equals(ORGANIZATION));

        return new Organization(objInf, names);
    }

    private Map<String, String> parseOrganizationName() throws XMLStreamException {
        validateElement(ORGANIZATION_NAME);
        Map<String, String> names = Collections.emptyMap();

        var depth = reader.getDepth();
        do {
            if (reader.next() == START_ELEMENT && reader.getName().equals(STRING)) {
                names = extractMultilingualStrings();
            }
        } while (depth <= reader.getDepth());

        return names;
    }

    private Map<String, String> extractMultilingualContent() throws XMLStreamException {
        return extractMultilingualObject(CONTENT);
    }

    private Map<String, String> extractMultilingualStrings() throws XMLStreamException {
        return extractMultilingualObject(STRING);
    }

    private Map<String, String> extractMultilingualObject(QName qname) throws XMLStreamException {
        validateElement(qname);

        var map = new HashMap<String, String>();

        // The depth is set at the level of the parent element
        int depth = reader.getDepth() - 1;

        // Current event
        int event = reader.getEventType();
        do {
            if (event == START_ELEMENT) {
                var language = reader.getAttributeValue(XMLConstants.XML_NS_URI, "lang");
                var elementText = reader.getElementText();
                map.put(language, elementText);
            }

            // Get next event
            event = reader.next();
        } while (depth <= reader.getDepth() && (event != START_ELEMENT || reader.getName().equals(STRING)));

        return map;
    }

    private Reference parseReference() throws XMLStreamException {
        // Check document position
        if (!reader.getLocalName().contains("Reference")) {
            throw new XMLStreamException("Unexpected element: " + reader.getName(), reader.getLocation());
        }

        // Stream to next element
        reader.nextTag();

        // Parse information
        var objInf = parseObjectInformation();
        String typeOfObject = null;
        if (reader.getName().equals(new QName(DDI_REUSABLE, "TypeOfObject"))) {
            typeOfObject = reader.getElementText();
        }

        return new Reference(objInf, typeOfObject);
    }

    private void validateElement(QName expectedElement) throws XMLStreamException {
        if (!expectedElement.equals(reader.getName())) {
            throw new XMLStreamException("Unexpected element: " + reader.getName(), reader.getLocation());
        }
    }

    /**
     * Parse DDI object information. The reader will be positioned at
     * the next START_ELEMENT encountered after this has returned.
     *
     * @return
     * @throws XMLStreamException
     */
    private ObjectInformation parseObjectInformation() throws XMLStreamException {

        String urn = null;
        String agency = null;
        String id = null;
        String version = null;

        do {
            // Find the next element
            if (reader.getEventType() != START_ELEMENT) {
                throw new XMLStreamException("Not a START_ELEMENT", reader.getLocation());
            }

            // Namespace guard
            if (!reader.getNamespaceURI().equals(DDI_REUSABLE)) {
                throw new XMLStreamException("Element " + reader.getName() + " was unexpected", reader.getLocation());
            }

            switch (reader.getLocalName()) {
                case "URN" -> urn = reader.getElementText();
                case "Agency" -> agency = reader.getElementText();
                case "ID" -> id = reader.getElementText();
                case "Version" -> version = reader.getElementText();
                default -> throw new XMLStreamException("Element " + reader.getName() + " was unexpected", reader.getLocation());
            }

        } while (LOCAL_NAMES.contains(reader.nextElement()));

        // Finalise object
        if (agency != null && id != null && version != null) {
            return new ObjectInformation(agency, id, version);
        }

        // If agency, id or version are not set, try to construct from the URN
        if (urn != null) {
            var urnMatch = DDI_URN_REGEX.matcher(urn);
            if (urnMatch.matches()) {
                var urnAgency = urnMatch.group(1);
                var urnId = urnMatch.group(2);
                var urnVersion = urnMatch.group(3);

                return new ObjectInformation(urnAgency, urnId, urnVersion);
            }
        }

        throw new IllegalStateException("Invalid ID");
    }
}
