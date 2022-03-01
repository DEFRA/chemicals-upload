package uk.gov.defra.reach.upload.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthContributorRegistry;
import org.springframework.context.annotation.Configuration;
import uk.gov.defra.reach.spring.health.BlobStorageHealthCheck;
import uk.gov.defra.reach.storage.Storage;

@Configuration
public class HealthCheckConfig {

  @Autowired
  public HealthCheckConfig(Storage storage, HealthContributorRegistry healthContributorRegistry) {
    healthContributorRegistry.registerContributor("Temporary blob container", new BlobStorageHealthCheck(storage));
  }

}
