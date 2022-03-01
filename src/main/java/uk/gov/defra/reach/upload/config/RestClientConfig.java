package uk.gov.defra.reach.upload.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class RestClientConfig {

  @Value("${reach.monitoring.url}")
  private String monitoringUrl;

  @Value("${reach.audit.url}")
  private String auditUrl;

  @Bean
  public RestTemplate monitoringRestTemplate(RestTemplateBuilder restTemplateBuilder) {
    return restTemplateBuilder.uriTemplateHandler(new DefaultUriBuilderFactory(monitoringUrl)).build();
  }

  @Bean
  public RestTemplate auditRestTemplate(RestTemplateBuilder restTemplateBuilder) {
    return restTemplateBuilder.uriTemplateHandler(new DefaultUriBuilderFactory(auditUrl)).build();
  }
}
