package eu.cessda.pasc.oci.configurations;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Extra Util configuration
 *
 * @author moses@doraventures.com
 */
@Configuration
@Slf4j
public class UtilitiesConfiguration {

  @Autowired
  PascOciConfig pascOciConfig;

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  public DocumentBuilder documentBuilder() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    return factory.newDocumentBuilder();
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate(getClientHttpRequestFactory());
  }

  private ClientHttpRequestFactory getClientHttpRequestFactory() {
    HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
    clientHttpRequestFactory.setConnectTimeout(pascOciConfig.getRestTemplateProps().getConnTimeout());
    clientHttpRequestFactory.setReadTimeout(pascOciConfig.getRestTemplateProps().getReadTimeout());
    clientHttpRequestFactory.setConnectionRequestTimeout(pascOciConfig.getRestTemplateProps()
        .getConnRequestTimeout());
    return clientHttpRequestFactory;
  }
}
