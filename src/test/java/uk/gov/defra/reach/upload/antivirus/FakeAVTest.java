package uk.gov.defra.reach.upload.antivirus;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Base64;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import uk.gov.defra.reach.antivirus.InfectionStatus;
import uk.gov.defra.reach.antivirus.ScanResult;

class FakeAVTest {

  private final FakeAV fakeAV = new FakeAV();

  @SneakyThrows
  @Test
  void shouldProcessCleanZipFile() {
    InputStream inputStream = getClass().getClassLoader().getResourceAsStream("files/dossierFile.i6z");
    ScanResult result = fakeAV.scan(inputStream);
    assertThat(result.getStatus()).isEqualTo(InfectionStatus.CLEAN);
  }

  @SneakyThrows
  @Test
  void shouldProcessInfectedZipFile() {
    InputStream base64Stream = getClass().getClassLoader().getResourceAsStream("files/infected-dossier.i6z.base64");
    ScanResult result = fakeAV.scan(Base64.getDecoder().wrap(base64Stream));
    assertThat(result.getStatus()).isEqualTo(InfectionStatus.INFECTED);
  }

}
