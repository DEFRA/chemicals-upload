package uk.gov.defra.reach.upload.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.defra.reach.storage.Storage;
import uk.gov.defra.reach.storage.azure.AzureBlobStorage;
import uk.gov.defra.reach.storage.azure.AzureBlobStorageConfiguration;
import uk.gov.defra.reach.storage.azure.CloudBlobContainerConnection;
import uk.gov.defra.reach.storage.azure.exception.StorageInitializationException;

@Configuration
public class BlobStorageConfig {

  @Value("${azure.storage.temp.connection}")
  private String tempConnectionString;

  @Value("${azure.storage.temp.container}")
  private String tempContainerName;

  @Bean
  public Storage tempStorage() throws StorageInitializationException {
    AzureBlobStorageConfiguration azureBlobStorageConfiguration = new AzureBlobStorageConfiguration(tempConnectionString, tempContainerName, Duration.ZERO);
    return new AzureBlobStorage(new CloudBlobContainerConnection(azureBlobStorageConfiguration).getContainer(), azureBlobStorageConfiguration);
  }
}
