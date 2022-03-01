package uk.gov.defra.reach.upload.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.defra.reach.upload.service.FileParameters;
import uk.gov.defra.reach.upload.service.UploadResult;
import uk.gov.defra.reach.upload.service.UploadService;

@ExtendWith(MockitoExtension.class)
class UploadServiceControllerTest {

  @InjectMocks
  private UploadServiceController uploadServiceController;

  @Mock
  private UploadService uploadService;

  @Captor
  private ArgumentCaptor<FileParameters> fileParametersCaptor;

  @SneakyThrows
  @Test
  void uploadReturnsResult() {
    String target = "the target";
    String mediaType = "the media type";
    MultipartFile file = Mockito.mock(MultipartFile.class);
    UploadResult uploadResult = new UploadResult(null, null);

    when(file.getInputStream()).thenReturn(dummyInputStream());
    when(uploadService.upload(fileParametersCaptor.capture())).thenReturn(uploadResult);

    UploadResult result = uploadServiceController.upload(target, file, mediaType);

    assertThat(result).isSameAs(uploadResult);
    FileParameters fileParameters = fileParametersCaptor.getValue();
    assertThat(fileParameters.getTarget()).isEqualTo(target);
    assertThat(fileParameters.getMediaType()).isEqualTo(mediaType);
    assertThat(fileParameters.getFilestreamSupplier().get()).hasSameContentAs(dummyInputStream());
  }

  private static InputStream dummyInputStream() {
    return new ByteArrayInputStream(new byte[]{1, 2, 3, 4});
  }

}
