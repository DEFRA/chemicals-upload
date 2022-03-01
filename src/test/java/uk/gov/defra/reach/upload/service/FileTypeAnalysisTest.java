package uk.gov.defra.reach.upload.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class FileTypeAnalysisTest {

  private final FileTypeAnalysis fileTypeAnalysis = new FileTypeAnalysis();

  @Test
  public void shouldDetectMediaType() throws IOException {
    assertThat(fileTypeAnalysis.detectMediaType(getInputStreamForFile("files/pdfFile.pdf"))).isEqualTo("application/pdf");
    assertThat(fileTypeAnalysis.detectMediaType(getInputStreamForFile("files/dossierFile.i6z"))).isEqualTo("application/zip");
  }

  private InputStream getInputStreamForFile(String file) {
    return getClass().getClassLoader().getResourceAsStream(file);
  }

}
