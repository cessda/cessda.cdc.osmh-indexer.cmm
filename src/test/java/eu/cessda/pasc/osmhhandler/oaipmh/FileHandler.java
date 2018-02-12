package eu.cessda.pasc.osmhhandler.oaipmh;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * File handling helper methods
 *
 * @author moses@doraventures.com
 */
@Component
@Slf4j
public class FileHandler {

  public String getFileWithUtil(String fileName) {

    String result = "";
    ClassLoader classLoader = getClass().getClassLoader();
    try {
      result = IOUtils.toString(classLoader.getResourceAsStream(fileName));
    } catch (IOException | NullPointerException e) {
      log.error("Could not read file successfully [{}]", e.getMessage());
    }
    return result;
  }
}
