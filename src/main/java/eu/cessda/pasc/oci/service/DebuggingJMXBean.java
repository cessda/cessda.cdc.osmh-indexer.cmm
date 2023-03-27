/*
 * Copyright © 2017-2023 CESSDA ERIC (support@cessda.eu)
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

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.settings.ClusterGetSettingsRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.stream.Collectors;

import static org.elasticsearch.client.RequestOptions.DEFAULT;

/**
 * Bean for JMX debugging
 *
 * @author moses AT doraventures DOT com
 */
@Component
@Slf4j
public class DebuggingJMXBean {

  private final RestHighLevelClient elasticsearchClient;

  @Autowired
  public DebuggingJMXBean(RestHighLevelClient elasticsearchClient) {
    this.elasticsearchClient = elasticsearchClient;
  }

  public String printElasticSearchInfo() throws IOException {
      var asMap = elasticsearchClient.cluster().getSettings(new ClusterGetSettingsRequest(), DEFAULT).getPersistentSettings().getAsGroups();
      String elasticsearchInfo = "Elasticsearch Client Settings: [\n" + asMap.entrySet().stream()
          .map(entry -> "\t" + entry.getKey() + "=" + entry.getValue() + "\n")
          .collect(Collectors.joining()) + "]";

      var healths = elasticsearchClient.cluster().health(new ClusterHealthRequest(), DEFAULT);

      elasticsearchInfo += "\nElasticsearch Cluster Details:\n" +
          "\tCluster Name [" + healths.getClusterName() + "]" +
          "\tNumberOfDataNodes [" + healths.getNumberOfDataNodes() + "]" +
          "\tNumberOfNodes [" + healths.getNumberOfNodes() + "]";
      return elasticsearchInfo;
  }
}
