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
package eu.cessda.pasc.oci.service;

import eu.cessda.pasc.oci.configurations.AppConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import java.util.Map;
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
    Map<String, Settings> asMap = client.settings().getAsGroups();
    String elasticsearchInfo = "Elasticsearch Client Settings: [\n" + asMap.entrySet().stream()
        .map(entry -> "\t" + entry.getKey() + "=" + entry.getValue() + "\n")
        .collect(Collectors.joining()) + "]";

    ClusterHealthResponse healths = client.admin().cluster().prepareHealth().get();
    client.admin().cluster().prepareHealth().get();

    elasticsearchInfo += "\nElasticsearch Cluster Details:\n" +
        "\tCluster Name [" + healths.getClusterName() + "]" +
        "\tNumberOfDataNodes [" + healths.getNumberOfDataNodes() + "]" +
        "\tNumberOfNodes [" + healths.getNumberOfNodes() + "]";

    return elasticsearchInfo;
  }

  @ManagedOperation(description = "Show which SP repo Endpoints are currently active.")
  public String printCurrentlyConfiguredRepoEndpoints() {
    String reposStr = appConfigProps.getEndpoints().getRepos().stream().map(repo -> "\t" + repo + "\n")
            .collect(Collectors.joining());
    return "Configured Repositories: [\n" + reposStr + "]";
  }
}
