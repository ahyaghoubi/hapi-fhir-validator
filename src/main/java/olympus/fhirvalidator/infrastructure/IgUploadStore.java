package olympus.fhirvalidator.infrastructure;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@ApplicationScoped
public class IgUploadStore {
  private static final String STAGED_PREFIX = "staged://";
  Path uploadDirOverride;

  public String stage(FileUpload upload) throws IOException {
    if (upload == null) {
      throw new IllegalArgumentException("missing uploaded file");
    }
    String fileName = safeFileName(upload.fileName());
    if (fileName.isBlank()) {
      throw new IllegalArgumentException("uploaded file must have a filename");
    }
    Path source = upload.uploadedFile();
    if (source == null || !Files.exists(source)) {
      throw new IllegalArgumentException("uploaded file payload is missing");
    }
    Path dir = ensureUploadDir();
    String token = UUID.randomUUID().toString();
    Path target = dir.resolve(token + "-" + fileName);
    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    return STAGED_PREFIX + target.getFileName();
  }

  public Path resolveStagedPath(String value) {
    if (value == null || !value.startsWith(STAGED_PREFIX)) {
      return null;
    }
    String fileName = value.substring(STAGED_PREFIX.length()).trim();
    if (fileName.isBlank() || fileName.contains("/") || fileName.contains("\\")) {
      throw new IllegalArgumentException("invalid staged IG reference");
    }
    Path p = ensureUploadDir().resolve(fileName).normalize();
    if (!Files.exists(p)) {
      throw new IllegalArgumentException("staged IG not found: " + value + " (resolved path: " + p + ")");
    }
    return p;
  }

  private Path ensureUploadDir() {
    try {
      Path dir = uploadDirOverride != null
          ? uploadDirOverride
          : Path.of(System.getProperty("java.io.tmpdir"), "hapi-fhir-validator-igs");
      Files.createDirectories(dir);
      return dir;
    } catch (IOException e) {
      throw new IllegalArgumentException("failed to initialize IG upload dir: " + e.getMessage());
    }
  }

  private static String safeFileName(String fileName) {
    if (fileName == null) return "";
    String normalized = fileName.replace("\\", "/");
    int idx = normalized.lastIndexOf('/');
    String base = idx >= 0 ? normalized.substring(idx + 1) : normalized;
    String lower = base.toLowerCase(Locale.ROOT);
    if (!(lower.endsWith(".tgz") || lower.endsWith(".json") || lower.endsWith(".xml"))) {
      throw new IllegalArgumentException("IG upload must be .tgz, .json, or .xml");
    }
    return base;
  }
}
