package uk.gov.defra.reach.upload.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.defra.reach.storage.Storage;

@SpringBootTest
@TestPropertySource("classpath:application-dev.properties")
@AutoConfigureMockMvc
class SecurityTest {

  @Autowired
  private MockMvc mockMvc;

  @TestConfiguration
  static class TestConfig {

    @Bean
    @Primary
    public Storage tempStorage() {
      Storage storage = Mockito.mock(Storage.class);
      when(storage.getHealthCheck()).thenReturn(() -> true);
      return storage;
    }

  }

  @Test
  void rootPathReturnsOk() throws Exception {
    mockMvc.perform(get("/"))
        .andExpect(status().isOk());
  }

  @Test
  void healthCheckReturnsOk() throws Exception {
    mockMvc.perform(get("/healthcheck"))
        .andExpect(status().isOk());
  }

}
