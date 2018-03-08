package eu.cessda.pasc.oci;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Java6BDDAssertions.then;

@RunWith(SpringRunner.class)
public class OCIApplicationTests extends AbstractSpringTestProfileContext {

  @Autowired
  private AppConfigurationProperties appConfigurationProperties;

  @Test
	public void contextLoads() {
    then(appConfigurationProperties).isNotNull();
	}

}
