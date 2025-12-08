package eu.cessda.pasc.oci.parser;

import lombok.Getter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;

public class TrackingXMLReader extends StreamReaderDelegate {

    // Reader state
    ArrayDeque<QName> elementStack = new ArrayDeque<>();
    @Getter
    int depth = 0;

    public TrackingXMLReader(XMLStreamReader reader) {
        super(reader);
    }

    @Override
    public int next() throws XMLStreamException {
        int event = super.next();

        switch (event) {
            case START_ELEMENT -> {
                depth += 1;
                elementStack.push(getName());
            }
            case END_ELEMENT -> {
                depth -= 1;
                var poppedElement = elementStack.pop();
                var currentElement = getName();
                assert Objects.equals(currentElement, poppedElement);
            }
        }

        return event;
    }

    @Override
    public String getElementText() throws XMLStreamException {

        if (getEventType() != START_ELEMENT) {
            throw new XMLStreamException(
                "parser must be on START_ELEMENT to read next text", getLocation());
        }
        int eventType = next();
        StringBuilder content = new StringBuilder();
        while (eventType != END_ELEMENT) {
            switch (eventType) {
                case CHARACTERS, CDATA, SPACE, ENTITY_REFERENCE -> content.append(getText());
                case PROCESSING_INSTRUCTION, COMMENT -> {
                    // skipping
                }
                case END_DOCUMENT ->
                    throw new XMLStreamException("unexpected end of document when reading element text content");
                case START_ELEMENT ->
                    throw new XMLStreamException("elementGetText() function expects text only element but START_ELEMENT was encountered.", getLocation());
                default -> throw new XMLStreamException("Unexpected event type " + eventType, getLocation());
            }
            eventType = next();
        }
        return content.toString();
    }

    @Override
    public int nextTag() throws XMLStreamException {

        int eventType = next();
        while ((eventType == CHARACTERS && isWhiteSpace()) // skip whitespace
            || (eventType == CDATA && isWhiteSpace())
            // skip whitespace
            || eventType == SPACE
            || eventType == PROCESSING_INSTRUCTION
            || eventType == COMMENT) {
            eventType = next();
        }

        if (eventType != START_ELEMENT && eventType != END_ELEMENT) {
            throw new XMLStreamException("expected START_ELEMENT or END_ELEMENT", getLocation());
        }

        return eventType;
    }

    /**
     * Skips any text node (CHARACTERS or CDATA), COMMENT, or PROCESSING_INSTRUCTION,
     * until a START_ELEMENT is reached. If other than white space characters, COMMENT,
     * PROCESSING_INSTRUCTION, START_ELEMENT, END_ELEMENT are encountered, an exception
     * is thrown. This method should be used when processing element-only content separated
     * by white space.
     *
     * @return the {@link QName} of the element.
     */
    public QName nextElement() throws XMLStreamException {
        int eventType = next();
        while (eventType != START_ELEMENT) {
            eventType = next();
        }
        return getName();
    }

    public List<QName> elementStack() {
        return List.copyOf(elementStack);
    }
}
