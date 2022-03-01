package uk.gov.defra.reach.upload.dto;

import java.util.UUID;
import lombok.Data;
import uk.gov.defra.reach.security.AuthenticatedUser;

@Data
public class AuditEvent {
  private String action;
  private String actionParams;
  private int httpCode;
  private UUID userId;
  private String role;
  private UUID legalEntityIdentifier;
  private String networkAddress;

  public AuditEvent(String action, String actionParams, int httpCode, String networkAddress, AuthenticatedUser user) {
    this.action = action;
    this.actionParams = actionParams;
    this.httpCode = httpCode;
    this.userId = user.getUser().getUserId();
    this.role = user.getRole().toString();
    this.legalEntityIdentifier = user.getLegalEntity() != null ? user.getLegalEntity().getAccountId() : null;
    this.networkAddress = networkAddress;
  }
}

