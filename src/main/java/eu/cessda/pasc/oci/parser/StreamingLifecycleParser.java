package eu.cessda.pasc.oci.parser;

import lombok.extern.slf4j.Slf4j;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

@Slf4j
public class StreamingLifecycleParser {

    // Namespaces
    private static final String DDI_INSTANCE = "ddi:instance:3_3";
    private static final String DDI_REUSABLE = "ddi:reusable:3_3";

    // QNames
    private static final QName FRAGMENT = new QName(DDI_INSTANCE, "Fragment");
    private static final QName FRAGMENT_INSTANCE = new QName(DDI_INSTANCE, "FragmentInstance");
    private static final QName TOP_LEVEL_REFERENCE = new QName(DDI_INSTANCE, "TopLevelReference");
    private static final QName ORGANIZATION = new QName(DDI_INSTANCE, "Organization");

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
        } while (reader.next() != XMLStreamConstants.END_DOCUMENT);
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
                    parseObject();
                    //parsedObjects.add(ddiObject);
                }
            }
        } while (reader.getDepth() >= initialDepth && reader.next() != XMLStreamConstants.END_DOCUMENT);

        log.warn(parsedObjects.toString());
    }

    // Parses the object contained within the DDI fragment
    private void parseObject() throws XMLStreamException {
        // Check document position
        validateElement(FRAGMENT);

        var fragmentElement = reader.nextElement();

        if (fragmentElement.equals(ORGANIZATION)) {
            parseOrganization();
        }
    }

    private void parseOrganization() throws XMLStreamException {
        validateElement(ORGANIZATION);

        // Stream to next element
        reader.nextTag();

        // Parse object information
        var objInf = parseObjectInformation();


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
            if (!reader.getNamespaceURI().equals("ddi:reusable:3_3")) {
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

    sealed interface DDIObject permits TopLevelReference {
    }

    record TopLevelReference(ObjectInformation reference, String typeOfObject) implements DDIObject {
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
}
