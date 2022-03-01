package uk.gov.defra.reach.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.defra.reach.antivirus.InfectionStatus.CLEAN;

import java.time.Instant;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.defra.reach.antivirus.ScanResult;
import uk.gov.defra.reach.monitoring.model.MonitoringEvent;
import uk.gov.defra.reach.monitoring.model.MonitoringEventDetails;
import uk.gov.defra.reach.security.AuthenticatedUser;
import uk.gov.defra.reach.security.LegalEntity;
import uk.gov.defra.reach.security.Role;
import uk.gov.defra.reach.security.User;
import uk.gov.defra.reach.storage.StorageFilename;

@ExtendWith(MockitoExtension.class)
class MonitoringServiceTest {

  private AuthenticatedUser authenticatedUser;

  private StorageFilename storageFilename;

  @Mock
  private RestTemplate monitoringRestTemplate;

  @InjectMocks
  private MonitoringService monitoringService;

  @BeforeEach
  @SneakyThrows
  void setup() {
    User user = new User();
    user.setUserId(UUID.randomUUID());
    authenticatedUser = new AuthenticatedUser(user, new LegalEntity(), Role.REGULATOR);
    storageFilename = StorageFilename.from("test/file/name");
    Authentication authentication = Mockito.mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(authenticatedUser);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  @Test
  void sendMalwareThreatDetected_sendsCorrectData() {
    ScanResult scanResult = ScanResult.builder().status(CLEAN).definitions(null).build();

    MonitoringEvent expected = new MonitoringEvent(
        "AAD/" + authenticatedUser.getUser().getUserId(),
        authenticatedUser.getUser().getUserId(),
        Instant.now(),
        "reach-upload-service",
        "0204",
        9,
        new MonitoringEventDetails(
            "CHEM-FILE-UPLOAD-MALWARE-THREAT",
            String.format("File: %s Uploaded and Blocked", storageFilename.get()),
            String.format("Symantec result: %s", scanResult.reportResult())
        )
    );

    monitoringService.sendMalwareThreatDetected(storageFilename, scanResult);

    assertMonitoringClientServiceCalledWith(expected);
  }

  @Test
  void sendMalwareScanClean_sendsCorrectData() {
    ScanResult scanResult = ScanResult.builder().status(CLEAN).definitions(null).build();

    MonitoringEvent expected = new MonitoringEvent(
        "AAD/" + authenticatedUser.getUser().getUserId(),
        authenticatedUser.getUser().getUserId(),
        Instant.now(),
        "reach-upload-service",
        "0204",
        0,
        new MonitoringEventDetails(
            "CHEM-FILE-UPLOAD-MALWARE-CLEAN",
            String.format("File: %s Uploaded and Allowed", storageFilename.get()),
            String.format("Symantec result: %s", scanResult.reportResult())
        )
    );

    monitoringService.sendMalwareScanClean(storageFilename, scanResult);

    assertMonitoringClientServiceCalledWith(expected);
  }

  @Test
  void sendFileUploadBlocked_sendsCorrectData() {
    MonitoringEvent expected = new MonitoringEvent(
        "AAD/" + authenticatedUser.getUser().getUserId(),
        authenticatedUser.getUser().getUserId(),
        Instant.now(),
        "reach-upload-service",
        "0210",
        5,
        new MonitoringEventDetails("CHEM-FILE-UPLOAD-BLOCKED", "File upload blocked", "the details")
    );

    monitoringService.sendFileUploadBlocked("the details");

    assertMonitoringClientServiceCalledWith(expected);
  }

  @Test
  void failureToSendEvent_isIgnored() {
    ScanResult scanResult = ScanResult.builder().status(CLEAN).definitions(null).build();

    when(monitoringRestTemplate.postForLocation(anyString(), any())).thenThrow(RestClientException.class);

    monitoringService.sendMalwareThreatDetected(storageFilename, scanResult);
  }

  private void assertMonitoringClientServiceCalledWith(MonitoringEvent expected) {
    ArgumentCaptor<MonitoringEvent> eventArgumentCaptor = ArgumentCaptor.forClass(MonitoringEvent.class);
    Mockito.verify(monitoringRestTemplate).postForLocation(eq("/event"), eventArgumentCaptor.capture());

    MonitoringEvent actual = eventArgumentCaptor.getValue();
    assertThat(actual.getUserId()).isEqualTo(expected.getUserId());
    assertThat(actual.getSessionId()).isEqualTo(expected.getSessionId());
    assertThat(actual.getPriority()).isEqualTo(actual.getPriority());
    assertThat(actual.getPmcCode()).isEqualTo(expected.getPmcCode());
    assertThat(actual.getComponent()).isEqualTo(expected.getComponent());
    assertThat(actual.getDetails()).isEqualTo(expected.getDetails());
  }
}

