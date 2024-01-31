/*
 * Copyright © 2017-2024 CESSDA ERIC (support@cessda.eu)
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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.GetClusterSettingsRequest;
import co.elastic.clients.elasticsearch.cluster.HealthRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Bean for JMX debugging
 *
 * @author moses AT doraventures DOT com
 */
@Component
@Slf4j
public class DebuggingJMXBean {

  private final ElasticsearchClient elasticsearchClient;

  @Autowired
  public DebuggingJMXBean(ElasticsearchClient elasticsearchClient) {
    this.elasticsearchClient = elasticsearchClient;
  }

  public String printElasticSearchInfo() throws IOException {
      var asMap = elasticsearchClient.cluster().getSettings(GetClusterSettingsRequest.of(g -> g)).persistent();
      String elasticsearchInfo = "Elasticsearch Client Settings: [\n" + asMap.entrySet().stream()
          .map(entry -> "\t" + entry.getKey() + "=" + entry.getValue() + "\n")
          .collect(Collectors.joining()) + "]";

      var healths = elasticsearchClient.cluster().health(HealthRequest.of(h -> h));

      elasticsearchInfo += "\nElasticsearch Cluster Details:\n" +
          "\tCluster Name [" + healths.clusterName() + "]" +
          "\tNumberOfDataNodes [" + healths.numberOfDataNodes() + "]" +
          "\tNumberOfNodes [" + healths.numberOfNodes() + "]";
      return elasticsearchInfo;
  }
}
