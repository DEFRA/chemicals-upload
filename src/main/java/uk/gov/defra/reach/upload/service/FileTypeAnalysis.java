package uk.gov.defra.reach.upload.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

/**
 * Utility for analysing file types
 */
@Component
public class FileTypeAnalysis {

  private static final Tika TIKA = initialiseTika();

  /**
   * Detects the media type of a file
   * @param inputStream input stream to read the file
   * @return the detected media type
   */
  public String detectMediaType(InputStream inputStream) throws IOException {
    try (BufferedInputStream bis = new BufferedInputStream(inputStream)) {
      return TIKA.detect(bis);
    }
  }

  private static Tika initialiseTika() {
    try {
      return new Tika(new TikaConfig(FileTypeAnalysis.class.getResourceAsStream("/tika-config.xml")));
    } catch (TikaException | IOException | SAXException e) {
      throw new IllegalStateException("Error initilasing Tika", e);
    }
  }

}

