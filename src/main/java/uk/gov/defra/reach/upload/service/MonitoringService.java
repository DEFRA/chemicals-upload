package uk.gov.defra.reach.upload.service;

import static uk.gov.defra.reach.security.Role.INDUSTRY_USER;
import static uk.gov.defra.reach.security.Role.REACH_MANAGER;
import static uk.gov.defra.reach.security.Role.REACH_READER;

import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import uk.gov.defra.reach.antivirus.ScanResult;
import uk.gov.defra.reach.monitoring.model.MonitoringEvent;
import uk.gov.defra.reach.monitoring.model.MonitoringEventDetails;
import uk.gov.defra.reach.security.AuthenticatedUser;
import uk.gov.defra.reach.storage.StorageFilename;

@Component
@Slf4j
public class MonitoringService {

  private static final String REACH_UPLOAD_SERVICE = "reach-upload-service";

  private final RestTemplate monitoringRestTemplate;

  public MonitoringService(RestTemplate monitoringRestTemplate) {
    this.monitoringRestTemplate = monitoringRestTemplate;
  }

  /**
   * Sends a {@link MonitoringEvent} when a malware threat is detected in an uploaded dossier
   * @param filename the name of the dossier file
   * @param scanResult the result of the malware scan
   */
  public void sendMalwareThreatDetected(StorageFilename filename, ScanResult scanResult) {
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    String userId = authenticatedUser.getUser().getUserId().toString();
    MonitoringEvent monitoringEvent = MonitoringEvent.builder()
        .userId(getUserId(authenticatedUser))
        .sessionId(UUID.fromString(userId))
        .dateTime(Instant.now())
        .component(REACH_UPLOAD_SERVICE)
        .priority(9)
        .pmcCode("0204")
        .details(
            MonitoringEventDetails.builder()
                .transactionCode("CHEM-FILE-UPLOAD-MALWARE-THREAT")
                .message(String.format("File: %s Uploaded and Blocked", filename.get()))
                .additionalInfo(String.format("Symantec result: %s", scanResult.reportResult()))
                .build()
        )
        .build();

    sendEvent(monitoringEvent);
  }

  /**
   * Sends a {@link MonitoringEvent} when no malware is detected in an uploaded dossier
   * @param filename the name of the dossier file
   * @param scanResult the result of the malware scan
   */
  public void sendMalwareScanClean(StorageFilename filename, ScanResult scanResult) {
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    String userId = authenticatedUser.getUser().getUserId().toString();
    MonitoringEvent monitoringEvent = MonitoringEvent.builder()
        .userId(getUserId(authenticatedUser))
        .sessionId(UUID.fromString(userId))
        .dateTime(Instant.now())
        .component(REACH_UPLOAD_SERVICE)
        .priority(0)
        .pmcCode("0204")
        .details(
            MonitoringEventDetails.builder()
                .transactionCode("CHEM-FILE-UPLOAD-MALWARE-CLEAN")
                .message(String.format("File: %s Uploaded and Allowed", filename.get()))
                .additionalInfo(String.format("Symantec result: %s", scanResult.reportResult()))
                .build()
        )
        .build();

    sendEvent(monitoringEvent);
  }

  public void sendFileUploadBlocked(String details) {
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    String userId = authenticatedUser.getUser().getUserId().toString();
    MonitoringEvent monitoringEvent = MonitoringEvent.builder()
        .userId(getUserId(authenticatedUser))
        .sessionId(UUID.fromString(userId))
        .dateTime(Instant.now())
        .component(REACH_UPLOAD_SERVICE)
        .priority(5)
        .pmcCode("0210")
        .details(
            MonitoringEventDetails.builder()
                .transactionCode("CHEM-FILE-UPLOAD-BLOCKED")
                .message("File upload blocked")
                .additionalInfo(details)
                .build()
        )
        .build();

    sendEvent(monitoringEvent);
  }

  private void sendEvent(MonitoringEvent monitoringEvent) {
    try {
      monitoringRestTemplate.postForLocation("/event", monitoringEvent);
    } catch (RestClientException e) {
      log.error(e.toString());
    }
  }

  private static String getUserId(AuthenticatedUser user) {
    if (user.getRole().equals(INDUSTRY_USER) || user.getRole().equals(REACH_MANAGER) || user.getRole().equals(REACH_READER)) {
      return "IDM/" + user.getUser().getUserId();
    } else {
      return "AAD/" + user.getUser().getUserId();
    }
  }
}
