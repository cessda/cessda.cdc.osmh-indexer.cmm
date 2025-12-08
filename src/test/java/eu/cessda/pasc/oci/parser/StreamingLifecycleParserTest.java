package eu.cessda.pasc.oci.parser;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import eu.cessda.pasc.oci.ResourceHandler;
import eu.cessda.pasc.oci.exception.IndexerException;
import org.json.JSONException;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.net.URISyntaxException;

public class StreamingLifecycleParserTest {
    @Test
    public void shouldReturnValidCMMStudyRecordFromAFullyFragmentRecord() throws IOException, ProcessingException, JSONException, IndexerException, URISyntaxException, XMLStreamException {
        // Given
        var recordXML = ResourceHandler.getResource("xml/ddi_3_3/compliant_fragments_cmm_ddi_3_3.xml");

        // When
        var parser = new StreamingLifecycleParser();

        var source = new StreamSource(recordXML.openStream(), recordXML.toString());

        parser.parseDDI(source);
    }
}
