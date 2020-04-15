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
package eu.cessda.pasc.oci.service.helpers;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Bean for JMX debugging
 *
 * @author moses AT doraventures DOT com
 */
@Component
@ManagedResource
@Slf4j
public class DebuggingJMXBean {

  private final ElasticsearchTemplate elasticsearchTemplate;
  private final AppConfigurationProperties appConfigProps;

  @Autowired
  public DebuggingJMXBean(ElasticsearchTemplate elasticsearchTemplate, AppConfigurationProperties appConfigProps) {
    this.elasticsearchTemplate = elasticsearchTemplate;
    this.appConfigProps = appConfigProps;
  }

  @SuppressWarnings("SameReturnValue")
  @ManagedOperation(description = "Prints to log the Elasticsearch server state.")
  public String printElasticSearchInfo() {

    Client client = elasticsearchTemplate.getClient();
    Map<String, String> asMap = client.settings().getAsMap();
    log.info("ElasticSearch Client Settings Details [\n{}].",
            asMap.entrySet().stream()
                    .map(entry -> "\n" + "*" + entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining()));

    ClusterHealthResponse healths = client.admin().cluster().prepareHealth().get();
    client.admin().cluster().prepareHealth().get();

    log.info("ElasticSearch Cluster Details: Cluster Name [{}] \n NumberOfDataNodes [{}] \n NumberOfNodes [{}] ] \n",
            healths.getClusterName(),
            healths.getNumberOfDataNodes(),
            healths.getNumberOfNodes());

    log.debug("ElasticSearch Cluster Nodes Report: Start--");
    AtomicInteger counter = new AtomicInteger(1);
    log.debug("NumberOfNodes [{}], Index Details Details:", healths.getNumberOfNodes());

    String msg = "Index [{}] [Current Index [{}] \t Status [{}] ]";
    healths.getIndices().values()
            .forEach(health -> log.debug(msg, counter.getAndIncrement(), health.getIndex(), health.getStatus()));

    return "Printed Health";
  }

  @ManagedOperation(description = "Show which SP repo Endpoints are currently active.")
  public String printCurrentlyConfiguredRepoEndpoints() {

    String reposStr = appConfigProps.getEndpoints().getRepos().stream().map(repo ->
            String.format("\t Repo [%s] url [%s] handler[%s] %n",
                    repo.getName(),
                    repo.getUrl(),
                    appConfigProps.getEndpoints().getHarvesters().get(repo.getHandler()).getUrl()
            )
    ).collect(Collectors.joining());
    log.info("Configured Repos: [\n{}]", reposStr);
    return reposStr;
  }
}
