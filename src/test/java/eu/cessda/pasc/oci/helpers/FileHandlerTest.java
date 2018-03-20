package eu.cessda.pasc.oci.helpers;

import org.junit.Test;

import static org.assertj.core.api.Java6BDDAssertions.then;


/**
 * @author moses@doraventures.com
 */
public class FileHandlerTest {

  @Test
  public void shouldReturnContentToAValidFilePath() {

    FileHandler fileHandler = new FileHandler();
    String recordUkds998 = fileHandler.getFileWithUtil("record_ukds_998.json");
    then(recordUkds998).isNotEmpty();
  }


  @Test
  public void shouldReturnEmptyContentToAInvalidValidFilePath() {

    FileHandler fileHandler = new FileHandler();
    String recordUkds998 = fileHandler.getFileWithUtil("does_not_exist.json");
    then(recordUkds998).isEmpty();
  }
}