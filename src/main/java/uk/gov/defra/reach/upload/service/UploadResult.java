package uk.gov.defra.reach.upload.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.defra.reach.antivirus.ScanResult;

/**
 * <p>
 * Conveys both:
 * <ul>
 *   <li>result of virus scanning the uploaded file,</li>
 *   <li>Azure-calculated checksum of the uploaded file (<b>only</b> if the file is not infected).</li>
 * </ul>
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UploadResult {
  private ScanResult scan;
  private String checksum;
}
