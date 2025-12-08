package eu.cessda.pasc.oci.parser;

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
    private static final String DDI_DATACOLLECTION = "ddi:datacollection:3_3";
    private static final String DDI_INSTANCE = "ddi:instance:3_3";
    private static final String DDI_REUSABLE = "ddi:reusable:3_3";

    // QNames
    private static final QName FRAGMENT = new QName(DDI_INSTANCE, "Fragment");
    private static final QName FRAGMENT_INSTANCE = new QName(DDI_INSTANCE, "FragmentInstance");
    private static final QName TOP_LEVEL_REFERENCE = new QName(DDI_INSTANCE, "TopLevelReference");

    private static final QName ORGANIZATION = new QName(DDI_ARCHIVE, "Organization");
    private static final QName ORGANIZATION_NAME = new QName(DDI_ARCHIVE, "OrganizationName");
    private static final QName ORGANIZATION_IDENTIFICATION = new QName(DDI_ARCHIVE, "OrganizationIdentification");

    private static final QName COLLECTION_EVENT = new QName(DDI_DATACOLLECTION, "CollectionEvent");
    private static final QName DATA_COLLECTION = new QName(DDI_DATACOLLECTION, "DataCollection");
    private static final QName DATA_COLLECTION_DATE = new QName(DDI_DATACOLLECTION, "DataCollectionDate");
    private static final QName METHODOLOGY = new QName(DDI_DATACOLLECTION, "Methodology");
    private static final QName METHODOLOGY_REFERENCE = new QName(DDI_DATACOLLECTION, "MethodologyReference");
    private static final QName MODE_OF_COLLECTION = new QName(DDI_DATACOLLECTION, "ModeOfCollection");
    private static final QName TIME_METHOD = new QName(DDI_DATACOLLECTION, "TimeMethod");
    private static final QName SAMPLING_PROCEDURE = new QName(DDI_DATACOLLECTION, "SamplingProcedure");
    private static final QName TYPE_OF_SAMPLING_PROCEDURE = new QName(DDI_DATACOLLECTION, "TypeOfSamplingProcedure");

    private static final QName CONTENT = new QName(DDI_REUSABLE, "Content");
    private static final QName STRING = new QName(DDI_REUSABLE, "String");

    // URN Matcher
    private static final Pattern DDI_URN_REGEX = Pattern.compile("^urn:ddi:([^:]*):([^:]*):([^:]*)$");
    private final ArrayList<DDIObject> parsedObjects = new ArrayList<>();
    private TrackingXMLReader reader;

    private List<DDIObject> parsedObjects() {
        return List.copyOf(parsedObjects);
    }

    public void parseDDI(Source source) throws XMLStreamException {
        // Create reader
        var streamReader = XMLInputFactory.newInstance().createXMLStreamReader(source);
        reader = new TrackingXMLReader(streamReader);

        // Get the root DDI element
        do {
            if (reader.getEventType() == START_ELEMENT) {
                if (reader.getName().equals(FRAGMENT_INSTANCE)) {
                    // Start parsing
                    parseFromRoot();
                }
            }
        } while (reader.next() != END_DOCUMENT);
    }

    private void parseFromRoot() throws XMLStreamException {
        int initialDepth = reader.getDepth();
        do {
            if (reader.getEventType() == START_ELEMENT) {
                // Switch on element name
                if (reader.getName().equals(TOP_LEVEL_REFERENCE)) {
                    var topLevel = parseTopLevelRef();
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
        }

        return null;
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
                if (reader.getName().equals(SAMPLING_PROCEDURE)) {
                    samplingProcedure = parseSamplingProcedure();
                } else if (reader.getName().equals(TIME_METHOD)) {
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
                if (elementName.equals(new QName(DDI_DATACOLLECTION, "TypeOfTimeMethod"))) {
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
                var elementName = reader.getName();
                if (elementName.equals(TYPE_OF_SAMPLING_PROCEDURE)) {
                    // Parse controlled vocabulary information
                    controlledVocabulary = parseControlledVocabularyInformation();

                    // Get mode of collection
                    typeOfSamplingProcedure = reader.getElementText();
                } else if (elementName.equals(new QName(DDI_REUSABLE, "Description"))) {
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

        ObjectInformation methodologyReference = null;
        CollectionEvent collectionEvent = null;

        do {
            if (reader.getEventType() == START_ELEMENT) {
                if (reader.getName().equals(COLLECTION_EVENT)) {
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
                                collectionDates = parseDateType();
                            } else if (reader.getName().equals(MODE_OF_COLLECTION)) {
                                // Parse mode of collection
                                var modeOfCollection = parseModeOfCollection();
                                modeOfCollections.add(modeOfCollection);
                            }
                        }
                        // Get next event
                        event = reader.next();
                    } while (depth <= reader.getDepth());

                    collectionEvent = new CollectionEvent(collectionEventObjInf, collectionDates, modeOfCollections);

                } else if (reader.getName().equals(METHODOLOGY_REFERENCE)) {
                    // Stream to the next element
                    reader.nextTag();

                    // Parse object information
                    methodologyReference = parseObjectInformation();
                }
            }
        } while (reader.next() != END_ELEMENT || !reader.getName().equals(DATA_COLLECTION));

        return new DataCollection(objInf, collectionEvent, methodologyReference);
    }

    private DateType parseDateType() throws XMLStreamException {
        validateElement(DATA_COLLECTION_DATE);

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
                throw new XMLStreamException("DateType cannot have SimpleDate and either StartDate or EndDate");
            }
            return new SimpleDateType(simpleDate);
        } else {
            if (startDate != null || endDate != null) {
                return new PeriodDateType(startDate, endDate);
            }
            throw new XMLStreamException("DateType has no dates");
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

    private TopLevelReference parseTopLevelRef() throws XMLStreamException {
        // Check document position
        validateElement(TOP_LEVEL_REFERENCE);

        // Stream to next element
        reader.nextTag();

        // Parse information
        var objInf = parseObjectInformation();
        var typeOfObject = reader.getElementText();

        return new TopLevelReference(objInf, typeOfObject);
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
                default ->
                    throw new XMLStreamException("Element " + reader.getName() + " was unexpected", reader.getLocation());
            }

        } while (ObjectInformation.LOCAL_NAMES.contains(reader.nextElement()));

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

    sealed interface DDIObject permits CollectionEvent, DataCollection, Methodology, Organization, TopLevelReference {
        ObjectInformation objInf();
    }

    record DataCollection(ObjectInformation objInf, CollectionEvent collectionEvent, ObjectInformation methodologyReference) implements DDIObject {
    }

    record Organization(ObjectInformation objInf, Map<String, String> names) implements DDIObject {
    }

    record TopLevelReference(ObjectInformation objInf, String typeOfObject) implements DDIObject {
    }

    record ObjectInformation(
        String agency,
        String id,
        String version
    ) {
        static Set<QName> LOCAL_NAMES = Set.of(
            new QName(DDI_REUSABLE, "URN"),
            new QName(DDI_REUSABLE, "Agency"),
            new QName(DDI_REUSABLE, "ID"),
            new QName(DDI_REUSABLE, "Version")
        );
    }

    interface DateType {
    }

    record SimpleDateType(String simpleDate) implements DateType {
    }

    record PeriodDateType(String startDate, String endDate) implements DateType {
    }

    record CollectionEvent(ObjectInformation objInf, DateType collectionDate, List<ModeOfCollection> modesOfCollection) implements DDIObject {
    }

    private record Methodology(ObjectInformation objInf, SamplingProcedure samplingProcedure, TimeMethod timeMethod) implements DDIObject {
    }

    record ControlledVocabulary(String id, String agencyName, String versionId) {
    }

    record ModeOfCollection(ObjectInformation objInf, ControlledVocabulary cv, String modeOfCollection) {
    }

    record TimeMethod(ObjectInformation objInf, ControlledVocabulary cv, String typeOfTimeMethod) {
    }

    record SamplingProcedure(ObjectInformation objInf, ControlledVocabulary cv, String typeOfSamplingProcedure, Map<String, String> content)  {
    }
}
