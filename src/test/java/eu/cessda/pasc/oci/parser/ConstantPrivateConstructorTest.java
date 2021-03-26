/*
 * Copyright © 2017-2021 CESSDA ERIC (support@cessda.eu)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.cessda.pasc.oci.parser;

import eu.cessda.pasc.oci.TimeUtility;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertTrue;


/**
 * @author moses AT doraventures DOT com
 */
public class ConstantPrivateConstructorTest {

  @Rule
  public final ExpectedException exception = ExpectedException.none();

  @Test
  public void timeUtilityShouldHaveAPrivateConstructor() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
    Constructor<TimeUtility> constructor = TimeUtility.class.getDeclaredConstructor();
    assertTrue("Constructor is not private", Modifier.isPrivate(constructor.getModifiers()));
    constructor.setAccessible(true);
    exception.expect(InvocationTargetException.class);
    constructor.newInstance();
  }
}
