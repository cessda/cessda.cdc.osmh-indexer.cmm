package eu.cessda.pasc.oci.service.helpers;

import eu.cessda.pasc.oci.configurations.PascOciConfig;
import eu.cessda.pasc.oci.models.configurations.Repo;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.health.ClusterIndexHealth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bean for JMX debugging
 *
 * @author moses@doraventures.com
 */
@ManagedResource
@Slf4j
public class DebuggingJMXBean {

  private ElasticsearchTemplate elasticsearchTemplate;
  private PascOciConfig pascOciConfig;

  @Autowired
  public DebuggingJMXBean(ElasticsearchTemplate elasticsearchTemplate, PascOciConfig pascOciConfig) {
    this.elasticsearchTemplate = elasticsearchTemplate;
    this.pascOciConfig = pascOciConfig;
  }

  @ManagedOperation(description = "Prints to log the Elasticsearch server state.")
  public String printElasticSearchInfo() {

    Client client = elasticsearchTemplate.getClient();
    Map<String, String> asMap = client.settings().getAsMap();
    log.info("ElasticSearch Client Settings Details [ \n ]",
        asMap.entrySet().stream()
            .map(entry -> "\n" + "*" + entry.getKey() + "=" + entry.getValue())
            .collect(Collectors.joining()));

    ClusterHealthResponse healths = client.admin().cluster().prepareHealth().get();
    client.admin().cluster().prepareHealth().get();
    log.info("ElasticSearch Cluster Details [ \n Cluster Name [{}] \n NumberOfDataNodes [{}] \n NumberOfNodes [{}] ] \n",
        healths.getClusterName(), healths.getNumberOfDataNodes(), healths.getNumberOfNodes());

    if (log.isDebugEnabled()) {
      log.debug("ElasticSearch Cluster Nodes Report: Start--");
      int counter = 1;
      log.debug("NumberOfNodes [{}], Index Details Details:", healths.getNumberOfNodes());
      for (ClusterIndexHealth health : healths.getIndices().values()) {
        log.debug("Index [{}] [Current Index [{}] \t NumberOfShards [{}] \t NumberOfReplicas [{}] \t Status [{}] ]",
            counter++, health.getIndex(), health.getNumberOfShards(), health.getNumberOfReplicas(), health.getStatus());
      }
    }
    return "Printed Health";
  }

  @ManagedOperation(description = "Show which SP repo Endpoints are currently active.")
  public String printCurrentlyConfiguredRepoEndpoints() {

    StringBuilder reposStrBuilder = new StringBuilder();
    for (Repo repo : pascOciConfig.getEndpoints().getRepos()) {
      reposStrBuilder.append(
          String.format("\t Repo [%s] url [%s] handler[%s] %n", repo.getName(), repo.getUrl(), repo.getHandler()));
    }
    String reposStr = reposStrBuilder.toString();
    log.info("Configured Repos: [\n{}]", reposStr);
    return reposStr;
  }
}
