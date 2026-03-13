package eu.cessda.pasc.oci.parser;

import eu.cessda.pasc.oci.ResourceHandler;
import eu.cessda.pasc.oci.models.lifecycle.*;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;

public class StreamingLifecycleParserTest {
    @Test
    public void shouldReturnValidCMMStudyRecordFromAFullyFragmentRecord() throws IOException, XMLStreamException {
        // Given
        var recordXML = ResourceHandler.getResource("xml/ddi_3_3/compliant_fragments_cmm_ddi_3_3.xml");

        // When
        var xmlInputFactory = XMLInputFactory.newFactory();
        var source = new StreamSource(recordXML.openStream(), recordXML.toString());

        // Parse the document
        var parser = StreamingLifecycleParser.parseDocument(xmlInputFactory, source);

        var objectsByType = parser.getObjectsByType();

        // Should parse all supported types of objects
        Assertions.assertThat(objectsByType).containsKeys(
                Organization.class,
                DataCollection.class,
                Methodology.class,
                StudyUnit.class,
                PhysicalInstance.class,
                Universe.class
        );
    }
}
