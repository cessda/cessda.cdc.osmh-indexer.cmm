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
package eu.cessda.pasc.oci.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;

/**
 * A Pasc Harvester Service implementation
 *
 * @author moses AT doraventures DOT com
 */
@Component
@Slf4j
public class PascHarvesterDao extends DaoBase implements HarvesterDao {


    @Autowired
    public PascHarvesterDao(HttpClient httpClient) {
        super(httpClient);
    }

    @Override
    public InputStream listRecordHeaders(URI finalUrl) throws IOException {
        return postForStringResponse(finalUrl);
    }

    @Override
    public InputStream getRecord(URI finalUrl) throws IOException {
        return postForStringResponse(finalUrl);
    }
}
