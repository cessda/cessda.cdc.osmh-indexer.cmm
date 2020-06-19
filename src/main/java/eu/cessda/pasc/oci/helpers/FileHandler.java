/*
 * Copyright © 2017-2019 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.helpers;

import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * File handling helper methods. These methods load files using the classloader.
 *
 * @author moses AT doraventures DOT com
 */
@Component
public class FileHandler {
    /**
     * Load the specified resource and return it as an {@link InputStream}.
     *
     * @param fileName the resource to load
     * @throws FileNotFoundException if the resource could not be found,
     *                               the resource is in a package that is not opened unconditionally,
     *                               or access to the resource is denied by the security manager.
     */
    public InputStream getFileAsStream(String fileName) throws FileNotFoundException {
        InputStream resource = getClass().getClassLoader().getResourceAsStream(fileName);
        if (resource == null) {
            throw new FileNotFoundException(fileName + " could not be found");
        }
        return resource;
    }

    /**
     * Load the specified resource and return it as an {@link String}.
     * The string is decoded using the {@link StandardCharsets#UTF_8} charset.
     *
     * @param fileName the resource to load
     * @throws FileNotFoundException if the resource could not be found,
     *                               the resource is in a package that is not opened unconditionally,
     *                               or access to the resource is denied by the security manager.
     */
    public String getFileAsString(String fileName) throws IOException {
        InputStream resource = getFileAsStream(fileName);
        return new String(resource.readAllBytes(), StandardCharsets.UTF_8);
    }
}
