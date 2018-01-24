package eu.cessda.pasc.osmhhandler.oaipmh.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import static org.apache.http.ssl.SSLContexts.custom;

/**
 * Extra Util configuration
 *
 * @author moses@doraventures.com
 */
@Configuration
public class UtilitiesConfiguration {

  @Autowired
  PaSCHandlerOaiPmhConfig paSCHandlerOaiPmhConfig;

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
  public RestTemplate restTemplate() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    if (paSCHandlerOaiPmhConfig.getRestTemplateProps().isVerifySSL()) {
      return new RestTemplate(getClientHttpRequestFactory());
    }
    return new RestTemplate(getClientHttpRequestFactoryWithoutSSL());
  }

  private ClientHttpRequestFactory getClientHttpRequestFactory() {
    HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
    clientHttpRequestFactory.setConnectTimeout(paSCHandlerOaiPmhConfig.getRestTemplateProps().getConnTimeout());
    clientHttpRequestFactory.setReadTimeout(paSCHandlerOaiPmhConfig.getRestTemplateProps().getReadTimeout());
    clientHttpRequestFactory.setConnectionRequestTimeout(paSCHandlerOaiPmhConfig.getRestTemplateProps()
        .getConnRequestTimeout());
    return clientHttpRequestFactory;
  }

   // FIXME:  A "temp" to work around untrusted certificate for UKDA oai-pmh endpoint
  /**
   * Builds a {@link ClientHttpRequestFactory} with ssl off.
   */
  private ClientHttpRequestFactory getClientHttpRequestFactoryWithoutSSL()
      throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {

    TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
    SSLContext sslContext = custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
    SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);
    CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(csf).build();

    HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
    requestFactory.setConnectTimeout(paSCHandlerOaiPmhConfig.getRestTemplateProps().getConnTimeout());
    requestFactory.setReadTimeout(paSCHandlerOaiPmhConfig.getRestTemplateProps().getReadTimeout());
    requestFactory.setConnectionRequestTimeout(paSCHandlerOaiPmhConfig.getRestTemplateProps().getConnRequestTimeout());
    requestFactory.setHttpClient(httpClient);
    return requestFactory;
  }
}
