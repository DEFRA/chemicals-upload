package uk.gov.defra.reach.upload.security;

import java.security.Security;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecurityManager {

  @Value("${reach.upload.dnsCacheTimeToLive}")
  private String dnsCacheTimeToLive;

  @PostConstruct
  public void init() {
    Security.setProperty("networkaddress.cache.ttl", dnsCacheTimeToLive);
    Security.setProperty("networkaddress.cache.negative.ttl", dnsCacheTimeToLive);
  }

}
