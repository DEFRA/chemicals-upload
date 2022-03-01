package uk.gov.defra.reach.upload.antivirus;

import static uk.gov.defra.reach.antivirus.InfectionStatus.CLEAN;
import static uk.gov.defra.reach.antivirus.InfectionStatus.INFECTED;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.ZipInputStream;
import uk.gov.defra.reach.antivirus.AntiVirus;
import uk.gov.defra.reach.antivirus.AntiVirusException;
import uk.gov.defra.reach.antivirus.FakeVirus;
import uk.gov.defra.reach.antivirus.ScanResult;
import uk.gov.defra.reach.antivirus.VirusDefinitions;

public class FakeAV implements AntiVirus {

  private static final VirusDefinitions DEFINITIONS = new VirusDefinitions("STUB", LocalDate.of(2000, 1, 1));

  private static final int MAXIMUM_UNZIP_RECURSION = 100;

  private static final byte[] FAKE_VIRUS = FakeVirus.create();

  @Override
  public ScanResult scan(InputStream inputStream) throws AntiVirusException {
    try {
      if (isInfected(inputStream, 0)) {
        return ScanResult.builder()
            .status(INFECTED)
            .definitions(DEFINITIONS)
            .infections(Collections.singletonList(FakeVirus.infection()))
            .build();
      } else {
        return ScanResult.builder().status(CLEAN).definitions(DEFINITIONS).build();
      }
    } catch (IOException e) {
      throw new AntiVirusException("Error within stubbed AV", e);
    }
  }

  private boolean isInfected(InputStream file, int depth) throws IOException, AntiVirusException {
    if (depth > MAXIMUM_UNZIP_RECURSION) {
      throw new AntiVirusException("Maximum zip recursion depth exceeded!");
    }
    BufferedInputStream bis = new BufferedInputStream(file);
    ZipInputStream zis = isZip(bis);
    if (zis != null) {
      do {
        if (isInfected(zis, depth + 1)) {
          return true;
        }
      } while (zis.getNextEntry() != null);
      return false;
    } else {
      return isFakeVirus(bis);
    }
  }

  private static boolean isFakeVirus(InputStream is) throws IOException {
    byte[] bytes = new byte[FAKE_VIRUS.length];
    if (is.read(bytes) > 0) {
      return Arrays.equals(bytes, FAKE_VIRUS);
    } else {
      return false;
    }
  }

  private static ZipInputStream isZip(InputStream is) throws IOException {
    is.mark(100);
    ZipInputStream zipInputStream = new ZipInputStream(is);
    if (zipInputStream.getNextEntry() != null) {
      return zipInputStream;
    }
    is.reset();
    return null;
  }
}
