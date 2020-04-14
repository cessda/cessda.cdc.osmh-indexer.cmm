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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.awaitility.Duration;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Java6BDDAssertions.then;
import static org.awaitility.Awaitility.await;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

/**
 * Default Embedded server for Integration Test
 *
 * @author moses AT doraventures DOT com
 */
@Slf4j
public class EmbeddedElasticsearchServer implements Closeable {

  private static final int TCP_PORT = 9390;
  private static final int HTTP_PORT = 9290;
  private static final Path DATA_DIRECTORY = Path.of("target", "elasticsearch-data");

  private final Node node;

  public EmbeddedElasticsearchServer() {

    NodeBuilder nodeBuilder = nodeBuilder();
    nodeBuilder.settings()
            .put("path.home", DATA_DIRECTORY.toString())
            .put("http.port", HTTP_PORT)
            .put("transport.tcp.port", TCP_PORT)
            .put("discovery.zen.ping.multicast.enabled", "false");

    node = nodeBuilder.clusterName("elasticsearch").node();

    try (Client client = node.client()) {
      ClusterHealthResponse clusterHealthResponse = client.admin().cluster()
              .prepareHealth()
              .setWaitForGreenStatus().get();

      then(node).isNotNull();
      then(node.isClosed()).isFalse();
      log.info("Started Node. Health is [{}]", clusterHealthResponse);

      Duration waitDuration = Duration.TWO_SECONDS;
      log.info("Waiting [{}ms] for the node to start", waitDuration.getValueInMS());
      await().atMost(waitDuration);
    }
  }

  public Client getClient() {
    return node.client();
  }

  @Override
  public void close() throws IOException {
    log.info("Closing Embedded Elasticsearch");
    node.close();
    FileUtils.deleteDirectory(DATA_DIRECTORY.toFile());
  }
}
