package uk.gov.defra.reach.upload.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.defra.reach.antivirus.InfectionStatus.CLEAN;
import static uk.gov.defra.reach.antivirus.InfectionStatus.INFECTED;
import static uk.gov.defra.reach.antivirus.InfectionStatus.SYMANTEC_FAILURE;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.defra.reach.antivirus.AntiVirus;
import uk.gov.defra.reach.antivirus.AntiVirusException;
import uk.gov.defra.reach.antivirus.Infection;
import uk.gov.defra.reach.antivirus.ScanResult;
import uk.gov.defra.reach.antivirus.VirusDefinitions;
import uk.gov.defra.reach.storage.Storage;
import uk.gov.defra.reach.storage.StorageFilename;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

  private static final String MEDIA_TYPE = "application/zip";

  private static final String STORAGE_FILENAME = "Test Storage";

  private static final String XFF = "127.0.0.1";

  private static final LocalDate NOW = LocalDate.now();

  @Mock
  private AntiVirus antiVirus;

  @Mock
  private Storage storage;

  @Mock
  private MonitoringService monitoringService;

  @Mock
  private AuditService auditService;

  @Mock
  private FileTypeAnalysis fileTypeAnalysis;

  @InjectMocks
  private UploadService uploadService;

  @Test
  void upload_ReturnsCleanUploadResult_WhenCleanFileUploaded() throws Exception {
    ScanResult clean = ScanResult.builder().status(CLEAN).definitions(new VirusDefinitions("1.0.0", NOW)).build();
    UploadResult expectedResult = new UploadResult(clean, convertChecksum("Checksum"));
    FileParameters testFileParameters = buildFileParameters(STORAGE_FILENAME);

    when(antiVirus.scan(testFileParameters.getFilestreamSupplier().get())).thenReturn(clean);
    when(storage.store(eq(testFileParameters.getFilestreamSupplier().get()), eq(StorageFilename.from(STORAGE_FILENAME)))).thenReturn("Checksum");
    when(fileTypeAnalysis.detectMediaType(any(InputStream.class))).thenReturn(MEDIA_TYPE);

    UploadResult actualResult = uploadService.upload(testFileParameters);

    compareScanResult(actualResult, expectedResult);
    assertThat(actualResult.getChecksum()).isEqualTo(expectedResult.getChecksum());
    verify(monitoringService).sendMalwareScanClean(StorageFilename.from(STORAGE_FILENAME), clean);
    verify(auditService).sendFileUploadAuditEvent(MEDIA_TYPE, clean);
  }

  @Test
  void upload_throwsFileUploadException_WhenInfectedFileUploaded() throws Exception {
    ScanResult infected = ScanResult.builder().status(INFECTED)
      .definitions(new VirusDefinitions("1.0.0", NOW))
      .infections(Collections.singletonList(new Infection("RE", "T-Virus"))).build();
    FileParameters testFileParameters = buildFileParameters(STORAGE_FILENAME);

    when(antiVirus.scan(testFileParameters.getFilestreamSupplier().get())).thenReturn(infected);

    assertThatExceptionOfType(ResponseStatusException.class)
        .isThrownBy(() -> uploadService.upload(testFileParameters))
        .matches(e -> e.getStatus() == HttpStatus.NOT_ACCEPTABLE);

    verify(monitoringService).sendMalwareThreatDetected(StorageFilename.from(STORAGE_FILENAME), infected);
    verify(auditService).sendFileUploadAuditEvent(MEDIA_TYPE, infected);
  }

  @Test
  void upload_throwsFileUploadException_WhenAVError() throws Exception {
    ScanResult symantecFailure = ScanResult.builder().status(SYMANTEC_FAILURE).definitions(new VirusDefinitions("1.0.0", NOW)).build();
    FileParameters testFileParameters = buildFileParameters(STORAGE_FILENAME);

    when(antiVirus.scan(testFileParameters.getFilestreamSupplier().get())).thenReturn(symantecFailure);
    assertThatExceptionOfType(ResponseStatusException.class)
        .isThrownBy(() -> uploadService.upload(testFileParameters))
        .matches(e -> e.getStatus() == HttpStatus.SERVICE_UNAVAILABLE);
    verify(auditService).sendFileUploadAuditEvent(MEDIA_TYPE, symantecFailure);
  }

  @Test
  void upload_ThrowsAntiVirusException_WhenAVException() throws Exception {
    String UNEXPECTED_AV_ERROR = "Unexpected error with AntiVirus service!";
    FileParameters testFileParameters = buildFileParameters(STORAGE_FILENAME);

    when(antiVirus.scan(testFileParameters.getFilestreamSupplier().get())).thenThrow(new AntiVirusException("AV Exception"));

    assertThatThrownBy(() -> uploadService.upload(testFileParameters))
      .isInstanceOf(ResponseStatusException.class).hasMessageContaining(UNEXPECTED_AV_ERROR);
  }

  @Test
  void upload_ThrowsInterruptedException_WhenAVException() throws Exception {
    String UPLOAD_INTERRUPT_ERROR = "Upload operation interrupted!";
    FileParameters testFileParameters = buildFileParameters(STORAGE_FILENAME);

    when(antiVirus.scan(testFileParameters.getFilestreamSupplier().get())).thenThrow(new InterruptedException("Exception"));

    assertThatThrownBy(() -> uploadService.upload(testFileParameters))
      .isInstanceOf(ResponseStatusException.class)
      .hasMessageContaining(UPLOAD_INTERRUPT_ERROR);
  }

  @Test
  void upload_ThrowsIOException_WhenStorageException() throws Exception {
    String CANNOT_READ_FILE = "Cannot read uploaded file!";
    FileParameters testFileParameters = buildFileParameters(STORAGE_FILENAME);
    ScanResult clean = ScanResult.builder().status(CLEAN).definitions(new VirusDefinitions("1.0.0", NOW)).build();
    when(antiVirus.scan(testFileParameters.getFilestreamSupplier().get())).thenReturn(clean);
    when(storage.store(eq(testFileParameters.getFilestreamSupplier().get()), eq(StorageFilename.from(STORAGE_FILENAME)))).thenThrow(new IOException("Exception"));
    when(fileTypeAnalysis.detectMediaType(any(InputStream.class))).thenReturn(MEDIA_TYPE);

    assertThatThrownBy(() -> uploadService.upload(testFileParameters))
      .isInstanceOf(ResponseStatusException.class)
      .hasMessageContaining(CANNOT_READ_FILE);
  }

  @Test
  void upload_ThrowsInvalidStorageNameException_WhenStorageNameIsEmpty() {
    String INVALID_TARGET = "Invalid target filename supplied!";
    FileParameters testFileParameters = buildFileParameters("");

    assertThatThrownBy(() -> uploadService.upload(testFileParameters))
      .isInstanceOf(ResponseStatusException.class)
      .hasMessageContaining(INVALID_TARGET);
  }

  @Test
  void upload_ThrowsException_WhenMediaTypeMismatch() throws Exception {
    FileParameters testFileParameters = buildFileParameters(STORAGE_FILENAME);
    ScanResult clean = ScanResult.builder().status(CLEAN).definitions(new VirusDefinitions("1.0.0", NOW)).build();
    when(antiVirus.scan(testFileParameters.getFilestreamSupplier().get())).thenReturn(clean);
    when(fileTypeAnalysis.detectMediaType(any(InputStream.class))).thenReturn("something/else");
    String expectedError = "Expected media type 'application/zip', but detected 'something/else'";
    assertThatThrownBy(() -> uploadService.upload(testFileParameters))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining(expectedError);

    verify(monitoringService).sendFileUploadBlocked(expectedError);
  }

  private String convertChecksum(String checksum) {
    byte[] byteCheckSum = Base64.getDecoder().decode(checksum);
    return BaseEncoding.base16().lowerCase().encode(byteCheckSum);
  }

  private void compareScanResult(UploadResult actual, UploadResult expected) {
    assertThat(actual.getScan().getStatus().toString()).isEqualTo(expected.getScan().getStatus().toString());

    assertThat(actual.getScan().getDefinitions().getVersion()).isEqualTo(expected.getScan().getDefinitions().getVersion());
    assertThat(actual.getScan().getDefinitions().getDate()).isEqualTo(expected.getScan().getDefinitions().getDate());

    List<Infection> actualInfections = Lists.newArrayList(actual.getScan().getInfections());
    List<Infection> expectedInfections = Lists.newArrayList(expected.getScan().getInfections());
    assertThat(actualInfections.size()).isEqualTo(expectedInfections.size());

    for (int i = 0; i < actualInfections.size(); i++) {
      assertThat(actualInfections.get(i).getName()).isEqualTo(expectedInfections.get(i).getName());
      assertThat(actualInfections.get(i).getId()).isEqualTo(expectedInfections.get(i).getId());
    }
  }

  private FileParameters buildFileParameters(String STORAGE_FILENAME) {
    InputStream inputStream = new ByteArrayInputStream(new byte[0]);
    return FileParameters
        .builder()
        .target(STORAGE_FILENAME)
        .filestreamSupplier(() -> inputStream)
        .mediaType(MEDIA_TYPE)
        .build();
  }
}
