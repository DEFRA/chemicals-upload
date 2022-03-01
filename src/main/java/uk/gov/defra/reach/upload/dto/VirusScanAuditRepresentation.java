package uk.gov.defra.reach.upload.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import uk.gov.defra.reach.antivirus.InfectionStatus;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
@Builder
public class VirusScanAuditRepresentation {

  private final String mediaType;

  private final String version;

  private final InfectionStatus infectionStatus;

  private final List<String> infection;

}
