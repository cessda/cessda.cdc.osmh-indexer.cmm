/*
 * Copyright © 2017-2025 CESSDA ERIC (support@cessda.eu)
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
package eu.cessda.pasc.oci.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;

import javax.xml.stream.XMLInputFactory;
import java.util.concurrent.ForkJoinPool;

/**
 * Extra Util configuration
 *
 * @author moses AT doraventures DOT com
 */
@Configuration
@Slf4j
public class UtilitiesConfiguration {

    @Bean("applicationTaskExecutor")
    ConcurrentTaskExecutor applicationTaskExecutor() {
        return new ConcurrentTaskExecutor(ForkJoinPool.commonPool());
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public XMLInputFactory xmlInputFactory() {
        return XMLInputFactory.newFactory();
    }
}
