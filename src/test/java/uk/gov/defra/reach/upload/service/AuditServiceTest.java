package uk.gov.defra.reach.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.defra.reach.antivirus.Infection;
import uk.gov.defra.reach.antivirus.InfectionStatus;
import uk.gov.defra.reach.antivirus.ScanResult;
import uk.gov.defra.reach.antivirus.VirusDefinitions;
import uk.gov.defra.reach.security.AuthenticatedUser;
import uk.gov.defra.reach.security.LegalEntity;
import uk.gov.defra.reach.security.Role;
import uk.gov.defra.reach.security.User;
import uk.gov.defra.reach.upload.dto.AuditEvent;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

  private static final AuthenticatedUser AUTHENTICATED_USER = new AuthenticatedUser(new User(), new LegalEntity(), Role.REACH_MANAGER);

  @InjectMocks
  private AuditService auditService;

  @Mock
  private RestTemplate auditRestTemplate;

  @Captor
  private ArgumentCaptor<AuditEvent> auditEventCaptor;

  @BeforeEach
  void setup() {
    mockSecurityPrincipal();
    mockRequestContext();
  }

  @SneakyThrows
  @Test
  void shouldSendCleanFileUploadAuditEvent() {
    ScanResult scanResult = ScanResult.builder()
        .definitions(new VirusDefinitions("v1", LocalDate.EPOCH))
        .status(InfectionStatus.CLEAN)
        .infections(null)
        .build();
    auditService.sendFileUploadAuditEvent("the media type", scanResult);
    verify(auditRestTemplate).postForLocation(eq("/audit"), auditEventCaptor.capture());

    AuditEvent auditEvent = auditEventCaptor.getValue();
    assertThat(auditEvent.getAction()).isEqualTo("uploadFile");
    assertThat(auditEvent.getActionParams()).contains("CLEAN");
    assertThat(auditEvent.getActionParams()).contains("v1");
  }

  @SneakyThrows
  @Test
  void shouldSendInfectedFileUploadAuditEvent() {
    ScanResult scanResult = ScanResult.builder()
        .definitions(new VirusDefinitions("v1", LocalDate.EPOCH))
        .status(InfectionStatus.INFECTED)
        .infections(List.of(new Infection("i1", "infection1")))
        .build();

    auditService.sendFileUploadAuditEvent("the media type", scanResult);
    verify(auditRestTemplate).postForLocation(eq("/audit"), auditEventCaptor.capture());

    AuditEvent auditEvent = auditEventCaptor.getValue();
    assertThat(auditEvent.getAction()).isEqualTo("uploadFile");
    assertThat(auditEvent.getActionParams()).contains("INFECTED");
    assertThat(auditEvent.getActionParams()).contains("infection1");
  }

  private static void mockSecurityPrincipal() {
    Authentication authentication = Mockito.mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(AUTHENTICATED_USER);
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }

  private static void mockRequestContext() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
  }

}
