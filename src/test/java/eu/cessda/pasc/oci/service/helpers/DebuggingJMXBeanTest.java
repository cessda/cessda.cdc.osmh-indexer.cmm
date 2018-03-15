package eu.cessda.pasc.oci.service.helpers;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import org.assertj.core.api.Java6BDDAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Java6BDDAssertions.then;

/**
 * @author moses@doraventures.com
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class DebuggingJMXBeanTest extends EmbeddedElasticsearchServer {

  // Class under test
  private DebuggingJMXBean debuggingJMXBean;
  @Autowired
  private AppConfigurationProperties appConfigurationProperties;

  @Before
  public void init() {
    startup(ELASTICSEARCH_HOME);
    ElasticsearchTemplate elasticsearchTemplate = new ElasticsearchTemplate(getClient());
    debuggingJMXBean = new DebuggingJMXBean(elasticsearchTemplate, appConfigurationProperties);
  }

  @After
  public void shutdown() {
    closeNodeResources();
    Java6BDDAssertions.then(this.node.isClosed()).isTrue();
  }

  @Test
  public void shouldPrintElasticsearchDetails() {
    then(debuggingJMXBean.printElasticSearchInfo()).isEqualTo("Printed Health");
  }

  @Test
  public void shouldPrintCurrentlyConfiguredRepoEndpoints() {
    // When
    String actualRepos = debuggingJMXBean.printCurrentlyConfiguredRepoEndpoints();
    then(actualRepos).isNotEmpty();
  }
}