package uk.gov.defra.reach.upload.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.defra.reach.antivirus.InfectionStatus;
import uk.gov.defra.reach.antivirus.ScanResult;
import uk.gov.defra.reach.security.AuthenticatedUser;
import uk.gov.defra.reach.upload.dto.AuditEvent;
import uk.gov.defra.reach.upload.dto.VirusScanAuditRepresentation;
import uk.gov.defra.reach.upload.dto.VirusScanAuditRepresentation.VirusScanAuditRepresentationBuilder;

@Component
public class AuditService {

  private static final ObjectMapper JSON_MAPPER = Jackson2ObjectMapperBuilder.json().build();

  private final RestTemplate auditRestTemplate;

  @Inject
  public AuditService(RestTemplate auditRestTemplate) {
    this.auditRestTemplate = auditRestTemplate;
  }

  public void sendFileUploadAuditEvent(String mediaType, ScanResult scanResult) throws IOException {
    AuditEvent auditEvent = buildAuditEvent(mediaType, scanResult);
    auditRestTemplate.postForLocation("/audit", auditEvent);
  }

  private static AuditEvent buildAuditEvent(String mediaType, ScanResult scanResult) throws JsonProcessingException {
    AuthenticatedUser authenticatedUser = (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    int code = scanResult.getStatus() == InfectionStatus.CLEAN ? HttpStatus.OK.value() : HttpStatus.NOT_ACCEPTABLE.value();
    return new AuditEvent("uploadFile", JSON_MAPPER.writeValueAsString(transform(mediaType, scanResult)), code, request.getHeader("x-forwarded-for"),
        authenticatedUser);
  }

  private static VirusScanAuditRepresentation transform(String mediaType, ScanResult scanResult) {
    VirusScanAuditRepresentationBuilder builder = VirusScanAuditRepresentation.builder()
        .mediaType(mediaType)
        .version(scanResult.getDefinitions().getVersion())
        .infectionStatus(scanResult.getStatus());

    if (scanResult.getInfections() != null) {
      List<String> infections = new ArrayList<>();
      scanResult.getInfections().forEach(infection -> infections.add(infection.getName()));
      builder.infection(infections);
    }
    return builder.build();
  }
}
