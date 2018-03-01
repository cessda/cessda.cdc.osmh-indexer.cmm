package eu.cessda.pasc.oci;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 *  "The Old InstanceAlreadyExistsException JUnit Trick"
 *  Base Context for tests to share to avoid UnableToRegisterMBeanException, InstanceAlreadyExistsException
 *
 * @author moses@doraventures.com
 */
@SpringBootTest()
@ActiveProfiles("test")
public class AbstractSpringTestProfileContext {
}
