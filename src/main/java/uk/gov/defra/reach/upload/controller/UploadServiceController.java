package uk.gov.defra.reach.upload.controller;

import java.io.InputStream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.defra.reach.upload.service.FileParameters;
import uk.gov.defra.reach.upload.service.UploadResult;
import uk.gov.defra.reach.upload.service.UploadService;

@RestController
@RequestMapping("/upload")
@Slf4j
public class UploadServiceController {

  private final UploadService uploadService;

  public UploadServiceController(UploadService uploadService) {
    this.uploadService = uploadService;
  }

  @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public UploadResult upload(@RequestParam("target") String target, @RequestParam("file") MultipartFile file, @RequestParam("mediaType") String mediaType) {
    return uploadService.upload(FileParameters.builder()
        .target(target)
        .mediaType(mediaType)
        .filestreamSupplier(() -> getFileInputStream(file))
        .build());
  }

  @SneakyThrows
  private InputStream getFileInputStream(MultipartFile file) {
    return file.getInputStream();
  }
}
