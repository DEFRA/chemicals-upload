package uk.gov.defra.reach.upload.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.defra.reach.file.Container;
import uk.gov.defra.reach.file.SerializableChecksum;
import uk.gov.defra.reach.file.SerializableUri;
import uk.gov.defra.reach.upload.service.UploadResult;

/**
 * End-to-end test designed to run against a deployed instance of reach-upload-service
 */
class UploadServiceIT {

  /**
   * JWT token for key MySecretKey valid until 2030
   */
  private static final String JWT_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJleHAiOjE5MjI3ODA2MDcsImxlZ2FsRW50aXR5Um9sZSI6IlJFR1VMQVRPUiIsInNvdXJjZSI6ImJsYWgiLCJ1c2VySWQiOiJkODllYmM4Ni1jMjNhLTQyODItYjVlMi01N2FiZWJhZWMzOTMiLCJjb250YWN0SWQiOm51bGwsImVtYWlsIjoicmVndWxhdG9yMUBlbWFpbC5jb20iLCJncm91cHMiOlsiYjQyNTAwYzctODBiZS00MjUxLWEwMjgtZDE3ZjQ1ODdiYjQ0Il0sInJvbGUiOiJSRUdVTEFUT1IiLCJ1c2VyIjpudWxsfQ.RGzbN9XmfIZvt8vdBT7pJEHvp6SU8Ru1i0FmZ16y080";

  private static final String SERVICE_URL = System.getProperty("UPLOAD_API", "http://localhost:8092");

  private static final RestTemplate REST_TEMPLATE = new RestTemplate(new SimpleClientHttpRequestFactory() {
    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
      ClientHttpRequest request = super.createRequest(uri, httpMethod);
      request.getHeaders().setBearerAuth(JWT_TOKEN);
      return request;
    }
  });

  @SneakyThrows
  @Test
  void uploadFile() {
    doUpload(generateTestFile(1000));
  }

  @SneakyThrows
  @Test
  void upload100MBFile() {
    doUpload(generateTestFile(1024 * 1024 * 100));
  }

  private void doUpload(Resource file) {
    String target = "test-" + UUID.randomUUID().toString();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.setBearerAuth(JWT_TOKEN);

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("file", file);
    body.add("mediaType", "application/octet-stream");
    body.add("target", target);
    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

    ResponseEntity<String> responseEntity = REST_TEMPLATE.postForEntity(SERVICE_URL + "/upload", requestEntity, String.class);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @SneakyThrows
  private static Resource generateTestFile(int size) {
    Path testFile = Files.createTempFile("test-file", ".txt");
    Files.write(testFile, RandomUtils.nextBytes(size));
    File file = testFile.toFile();
    file.deleteOnExit();
    return new FileSystemResource(file);
  }

}
