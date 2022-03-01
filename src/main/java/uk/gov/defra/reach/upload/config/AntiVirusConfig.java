package uk.gov.defra.reach.upload.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.defra.reach.antivirus.AntiVirus;
import uk.gov.defra.reach.antivirus.symantec.SymantecAntiVirus;
import uk.gov.defra.reach.upload.antivirus.FakeAV;

@Configuration
@EnableConfigurationProperties(SymantecConfig.class)
public class AntiVirusConfig {

  @Bean
  @ConditionalOnProperty(name = "reach.upload.stubAv")
  public AntiVirus stubAntiVirus() {
    return new FakeAV();
  }

  @Bean
  @ConditionalOnProperty(name = "reach.upload.stubAv", havingValue = "false", matchIfMissing = true)
  public AntiVirus symantecAntiVirus(SymantecConfig symantecConfig) {
    return new SymantecAntiVirus(symantecConfig);
  }
}
