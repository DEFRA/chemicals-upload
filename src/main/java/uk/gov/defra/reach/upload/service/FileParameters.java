package uk.gov.defra.reach.upload.service;

import java.io.InputStream;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Data;

/**
 * Encapsulates file upload path parameters
 */
@Data
@Builder
public class FileParameters {
  private final Supplier<InputStream> filestreamSupplier;
  private final String target;
  private final String mediaType;
}

