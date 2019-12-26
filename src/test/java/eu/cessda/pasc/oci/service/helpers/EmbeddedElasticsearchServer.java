/*
# Copyright CESSDA ERIC 2017-2019
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.
# You may obtain a copy of the License at
# http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
*/
package eu.cessda.pasc.oci.service.helpers;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Java6BDDAssertions.then;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;
import static org.junit.Assert.fail;

/**
 * Default Embedded server for Integration Test
 *
 * @author moses AT doraventures DOT com
 */
@Slf4j
public class EmbeddedElasticsearchServer {

  private static final int TCP_PORT = 9390;
  private static final int HTTP_PORT = 9290;
  protected static final String ELASTICSEARCH_HOME = "target/elasticsearch-data";
  protected Node node;
  private String dataDirectory;

  protected void startup(String dataDirectory) {

    this.dataDirectory = dataDirectory;
    NodeBuilder nodeBuilder = nodeBuilder();
    nodeBuilder.settings()
        .put("path.home", dataDirectory)
        .put("http.port", HTTP_PORT)
        .put("transport.tcp.port", TCP_PORT)
        .put("discovery.zen.ping.multicast.enabled", "false");

    node = nodeBuilder.clusterName("elasticsearch").node();

    ClusterHealthResponse clusterHealthResponse = node.client().admin().cluster()
        .prepareHealth()
        .setWaitForGreenStatus().get();

    then(node).isNotNull();
    then(node.isClosed()).isFalse();
    log.info("Started Node and health is [{}]", clusterHealthResponse);

    try {
      int waitMillis = 2_000;
      log.info("Waiting [{}ms] for node to fully initiate (For low spec environment)", waitMillis);
      Thread.sleep(waitMillis); //TODO: replace with Awaitility
    } catch (InterruptedException e) {
      log.error("Unable to Sleep [{}]", e.getMessage(), e);
      log.info("Unable to Sleep: Re-interrupting", e.getMessage(), e);
      Thread.interrupted();
    }
  }

  protected Client getClient() {
    return node.client();
  }

  protected void closeNodeResources() {
    try {
      node.close();
      FileUtils.deleteDirectory(new File(dataDirectory));
      log.info("Deleted data Directory");
    } catch (IOException e) {
      log.error("Unable to delete data Directory [{}]", e.getMessage(), e);
      fail("Unable to delete Directory and wait successfully");
    }
  }
}
