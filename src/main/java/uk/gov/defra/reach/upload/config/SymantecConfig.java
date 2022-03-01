package uk.gov.defra.reach.upload.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import uk.gov.defra.reach.antivirus.symantec.SymantecAntiVirusConfiguration;

@ConfigurationProperties(prefix = "reach.upload.symantec")
@Data
public class SymantecConfig implements SymantecAntiVirusConfiguration {

  private String host;

  private Integer port;

  private Integer maximumConnectionAttempts;

  private Integer retryDelay;

  private Integer socketTimeout;

}
