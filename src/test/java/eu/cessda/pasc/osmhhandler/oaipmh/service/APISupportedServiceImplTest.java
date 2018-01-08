package eu.cessda.pasc.osmhhandler.oaipmh.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class APISupportedServiceImplTest {

  @Autowired
  APISupportedServiceImpl apiSupportedServiceImpl;

  @Test
  public void shouldReturnTheListOfSupportedServices() {

    List<String> supportedApiVersions = apiSupportedServiceImpl.getSupportedVersion();

    then(supportedApiVersions).contains("v0");
    then(supportedApiVersions).hasSize(1);
  }

  @Test
  public void shouldReturnTrueForSupportedAPIVersion() {
    boolean isSupportedVersion = apiSupportedServiceImpl.isSupportedVersion("v0");
    then(isSupportedVersion).isTrue();
  }

  @Test
  public void shouldReturnFalseForUnSupportedAPIVersion() {
    boolean isSupportedVersion = apiSupportedServiceImpl.isSupportedVersion("v2");
    then(isSupportedVersion).isFalse();
  }

  @Test
  public void shouldReturnTheListOfSupportedRecordTypes() {
    List<String> supportedRecordTypes = apiSupportedServiceImpl.getSupportedRecordTypes();

    then(supportedRecordTypes).contains("StudyGroup", "Study", "Variable", "Question", "CMM");
    then(supportedRecordTypes).hasSize(5);
  }
}