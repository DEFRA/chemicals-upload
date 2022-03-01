package uk.gov.defra.reach.upload.service;

import com.google.common.io.BaseEncoding;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.defra.reach.antivirus.AntiVirus;
import uk.gov.defra.reach.antivirus.AntiVirusException;
import uk.gov.defra.reach.antivirus.ScanResult;
import uk.gov.defra.reach.storage.InvalidStorageFilenameException;
import uk.gov.defra.reach.storage.Storage;
import uk.gov.defra.reach.storage.StorageFilename;

@Slf4j
@Service
public class UploadService {

  private static final String VIRUS_SCAN_RESULTS_PROGRESS = "Virus scan results: {} after {} milliseconds";
  private static final String CANNOT_READ_FILE = "Cannot read uploaded file!";
  private static final String MEDIA_TYPE_MISMATCH = "Expected media type '%s', but detected '%s'";
  private static final String INVALID_FILE_NAME = "Invalid target filename supplied!";
  private static final String START_VIRUS_SCAN = "Attempting virus scan for \"{}\"";
  private static final String UNEXPECTED_AV_ERROR = "Unexpected error with AntiVirus service!";
  private static final String UPLOAD_INTERRUPT_ERROR = "Upload operation interrupted!";
  private static final String FILE_UPLOADED = "File {} successfully upload to TEMPORARY storage after {} milliseconds";
  private static final Base64.Decoder BASE_64_DECODER = Base64.getDecoder();

  private final Storage storage;
  private final AntiVirus antiVirus;
  private final MonitoringService monitoringService;
  private final AuditService auditService;
  private final FileTypeAnalysis fileTypeAnalysis;

  public UploadService(Storage storage, AntiVirus antiVirus, MonitoringService monitoringService,
      AuditService auditService, FileTypeAnalysis fileTypeAnalysis) {
    this.storage = storage;
    this.antiVirus = antiVirus;
    this.monitoringService = monitoringService;
    this.auditService = auditService;
    this.fileTypeAnalysis = fileTypeAnalysis;
  }

  public UploadResult upload(FileParameters fileParameters) {
    try {
      StorageFilename filename = StorageFilename.from(fileParameters.getTarget());
      log.info(START_VIRUS_SCAN, filename);
      ScanResult scanResult = scanFile(fileParameters.getFilestreamSupplier().get());
      auditService.sendFileUploadAuditEvent(fileParameters.getMediaType(), scanResult);

      switch (scanResult.getStatus()) {
        case CLEAN:
          assertMediaTypeIsCorrect(fileParameters);
          byte[] checksum = storeFile(filename, fileParameters);
          monitoringService.sendMalwareScanClean(filename, scanResult);
          return new UploadResult(scanResult, BaseEncoding.base16().lowerCase().encode(checksum));
        case INFECTED:
          monitoringService.sendMalwareThreatDetected(filename, scanResult);
          throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Infection found in uploaded dossier file");
        case SYMANTEC_FAILURE:
          throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Symantec service failed");
        default:
          throw new IllegalStateException();
      }
    } catch (IOException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, CANNOT_READ_FILE, e);
    } catch (InvalidStorageFilenameException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_FILE_NAME, e);
    } catch (AntiVirusException e) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, UNEXPECTED_AV_ERROR, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, UPLOAD_INTERRUPT_ERROR, e);
    }
  }

  private void assertMediaTypeIsCorrect(FileParameters fileParameters) throws IOException {
    String detectedType = fileTypeAnalysis.detectMediaType(fileParameters.getFilestreamSupplier().get());
    if (!detectedType.equals(fileParameters.getMediaType())) {
      String errorDetails = String.format(MEDIA_TYPE_MISMATCH, fileParameters.getMediaType(), detectedType);
      monitoringService.sendFileUploadBlocked(errorDetails);
      throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, errorDetails);
    }
  }

  private byte[] storeFile(StorageFilename filename, FileParameters file) throws IOException {
    Instant persistBegin = Instant.now();
    String checksumString = storage.store(file.getFilestreamSupplier().get(), filename);
    final byte[] checksum = BASE_64_DECODER.decode(checksumString);
    Duration persistElapsed = Duration.between(persistBegin, Instant.now());
    log.info(FILE_UPLOADED, filename, persistElapsed.toMillis());
    return checksum;
  }

  private ScanResult scanFile(InputStream file) throws InterruptedException, AntiVirusException {
    Instant scanBegin = Instant.now();
    ScanResult scanResult = antiVirus.scan(file);
    Duration scanElapsed = Duration.between(scanBegin, Instant.now());
    log.info(VIRUS_SCAN_RESULTS_PROGRESS, scanResult.toString(), scanElapsed.toMillis());
    return scanResult;
  }
}
