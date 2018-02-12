package eu.cessda.pasc.osmhhandler.oaipmh.helpers;

import eu.cessda.pasc.osmhhandler.oaipmh.FileHandler;
import org.junit.Test;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author moses@doraventures.com
 */
public class FileHandlerTest {

  @Test
  public void shouldReturnEmptyStringForBadPath() {

    // Given
    FileHandler fileHandler = new FileHandler();

    // When
    String fileContent = fileHandler.getFileWithUtil("xml/ddi_record_1683_does_not_exist.xml");

    then(fileContent).isEqualTo("");
  }

  @Test
  public void shouldReturnContentOfValidFilePath() {

    // Given
    FileHandler fileHandler = new FileHandler();

    // When
    String fileContent = fileHandler.getFileWithUtil("xml/ddi_record_1683.xml");

    then(fileContent).contains("<request verb=\"GetRecord\" identifier=\"1683\"");
  }
}