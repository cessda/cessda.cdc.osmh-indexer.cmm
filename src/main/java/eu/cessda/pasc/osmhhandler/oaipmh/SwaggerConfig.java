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

package eu.cessda.pasc.osmhhandler.oaipmh;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.paths.RelativePathProvider;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.servlet.ServletContext;
import java.util.ArrayList;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

    private final String baseEndpoint;

    @Autowired
    public SwaggerConfig(@Value("${osmhhandler.baseEndpoint}") String baseEndpoint) {
        this.baseEndpoint = baseEndpoint;
    }

    @Bean
    public Docket api(ServletContext servletContext) {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("eu.cessda.pasc.osmhhandler.oaipmh.controller"))
                .build()
                .useDefaultResponseMessages(false)
                .pathProvider(new RelativePathProvider(servletContext) {
                    @Override
                    public String getApplicationBasePath() {
                        return baseEndpoint;
                    }
                })
                .apiInfo(metaData());
    }

    private ApiInfo metaData() {

        Contact contact = new Contact("Cessda PaSC", "https://www.cessda.eu/", "support@cessda.eu");
    String version = getClass().getPackage().getImplementationVersion();

    return new ApiInfo(
        "PaSC OSMH Handler OAI-PMH",
        "Cessda PaSC OSMH repository handler for harvesting OAI-PMH metadata format",
        version,
        "https://www.ukdataservice.ac.uk/conditions",
        contact,
        "Apache License Version 2.0",
        "https://www.apache.org/licenses/LICENSE-2.0",
        new ArrayList<>());
  }
}