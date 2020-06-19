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
import eu.cessda.pasc.oci.exception.HandlerNotFoundException;
import eu.cessda.pasc.oci.models.configurations.Harvester;
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

  @ManagedOperation(description = "Prints to log the Elasticsearch server state.")
  public String printElasticSearchInfo() {

    Client client = elasticsearchTemplate.getClient();
    Map<String, String> asMap = client.settings().getAsMap();
    String elasticsearchInfo = String.format("ElasticSearch Client Settings Details: [%n%s]", asMap.entrySet().stream()
            .map(entry -> "\t" + entry.getKey() + "=" + entry.getValue() + "\n")
            .collect(Collectors.joining())
    );

    ClusterHealthResponse healths = client.admin().cluster().prepareHealth().get();
    client.admin().cluster().prepareHealth().get();

    elasticsearchInfo += String.format("%nElasticSearch Cluster Details:%n\tCluster Name [%s]\tNumberOfDataNodes [%s]\tNumberOfNodes [%s]",
            healths.getClusterName(),
            healths.getNumberOfDataNodes(),
            healths.getNumberOfNodes()
    );

    if (log.isDebugEnabled()) {
      log.debug("ElasticSearch Cluster Nodes Report: Start--");
      AtomicInteger counter = new AtomicInteger(1);
      log.debug("NumberOfNodes [{}], Index Details Details:", healths.getNumberOfNodes());

      String msg = "Index [{}] [Current Index [{}] \t Status [{}] ]";
      healths.getIndices().values().forEach(health -> log.debug(msg, counter.getAndIncrement(), health.getIndex(), health.getStatus()));
    }

    return elasticsearchInfo;
  }

  @ManagedOperation(description = "Show which SP repo Endpoints are currently active.")
  public String printCurrentlyConfiguredRepoEndpoints() {
    String reposStr = appConfigProps.getEndpoints().getRepos().stream().map(repo -> {
              String handler;

              if (repo.getHandler().equalsIgnoreCase("OAI-PMH")) {
                handler = "Local";
              } else {
                Harvester harvester = appConfigProps.getEndpoints().getHarvesters().get(repo.getHandler().toUpperCase());
                if (harvester != null) {
                  handler = harvester.getUrl().toString();
                } else {
                  // No handler is configured, fail fast
                  throw new HandlerNotFoundException(repo);
                }
              }

              return String.format("\t Repo [%s] url [%s] handler[%s] %n",
                      repo.getName(),
                      repo.getUrl(),
                      handler
              );
            }
    ).collect(Collectors.joining());
    return String.format("Configured Repos: [%n%s]", reposStr);
  }
}
