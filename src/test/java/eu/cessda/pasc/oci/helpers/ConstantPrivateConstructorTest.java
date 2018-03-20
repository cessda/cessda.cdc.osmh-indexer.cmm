package eu.cessda.pasc.oci.helpers;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertTrue;


/**
 * @author moses@doraventures.com
 */
public class ConstantPrivateConstructorTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void logHelper_shouldHaveAPrivateConstructor() throws Exception {
    Constructor constructor = LogHelper.class.getDeclaredConstructor();
    assertTrue("Constructor is not private", Modifier.isPrivate(constructor.getModifiers()));
    constructor.setAccessible(true);
    exception.expect(InvocationTargetException.class);
    constructor.newInstance();
  }

  @Test
  public void appConstantsShouldHaveAPrivateConstructor() throws Exception {
    Constructor constructor = AppConstants.class.getDeclaredConstructor();
    assertTrue("Constructor is not private", Modifier.isPrivate(constructor.getModifiers()));
    constructor.setAccessible(true);
    exception.expect(InvocationTargetException.class);
    constructor.newInstance();
  }

  @Test
  public void timeUtilityShouldHaveAPrivateConstructor() throws Exception {
    Constructor constructor = TimeUtility.class.getDeclaredConstructor();
    assertTrue("Constructor is not private", Modifier.isPrivate(constructor.getModifiers()));
    constructor.setAccessible(true);
    exception.expect(InvocationTargetException.class);
    constructor.newInstance();
  }
}