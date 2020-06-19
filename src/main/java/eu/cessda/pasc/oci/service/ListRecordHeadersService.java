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

package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.exception.CustomHandlerException;
import eu.cessda.pasc.oci.models.RecordHeader;

import java.net.URI;
import java.util.List;

/**
 * Service interface contract to handle Listing Record Headers
 *
 * @author moses AT doraventures DOT com
 */
public interface ListRecordHeadersService {
    List<RecordHeader> getRecordHeaders(URI baseRepoUrl) throws CustomHandlerException;
}
