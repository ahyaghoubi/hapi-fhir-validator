package olympus.fhirvalidator.infrastructure;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import olympus.fhirvalidator.domain.ValueRow;

@ApplicationScoped
public class IgSourceResolver {
  @Inject
  IgUploadStore uploadStore;

  @ConfigProperty(name = "validator.ig.url-timeout-ms", defaultValue = "30000")
  long urlTimeoutMs;

  public ResolvedSources resolve(List<ValueRow> rows) {
    List<String> resolved = new ArrayList<>();
    List<Path> cleanup = new ArrayList<>();
    if (rows == null) return new ResolvedSources(resolved, cleanup);
    for (ValueRow row : rows) {
      if (row == null || row.value == null) continue;
      String value = row.value.trim();
      if (value.isEmpty()) continue;
      if (value.startsWith("http://") || value.startsWith("https://")) {
        Path downloaded = downloadUrl(value);
        resolved.add(downloaded.toString());
        cleanup.add(downloaded);
        continue;
      }
      Path staged = uploadStore.resolveStagedPath(value);
      if (staged != null) {
        resolved.add(staged.toString());
        // Staged uploads are one-time use and must be removed after validate attempt.
        cleanup.add(staged);
      } else {
        resolved.add(value);
      }
    }
    return new ResolvedSources(resolved, cleanup);
  }

  public void cleanup(ResolvedSources sources) {
    if (sources == null || sources.tempFiles().isEmpty()) return;
    for (Path p : sources.tempFiles()) {
      try {
        Files.deleteIfExists(p);
      } catch (IOException ignored) {
        // Best-effort cleanup for temporary IG files (URL downloads + staged uploads).
      }
    }
  }

  private Path downloadUrl(String sourceUrl) {
    try {
      URI uri = URI.create(sourceUrl);
      HttpClient client = HttpClient.newHttpClient();
      HttpRequest request = HttpRequest.newBuilder(uri)
          .timeout(Duration.ofMillis(Math.max(1000L, urlTimeoutMs)))
          .GET()
          .build();
      HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() < 200 || response.statusCode() > 299) {
        throw new IllegalArgumentException("failed to fetch IG URL " + sourceUrl + " (HTTP " + response.statusCode() + ")");
      }
      String extension = guessExtension(uri.getPath());
      Path target = Path.of(System.getProperty("java.io.tmpdir"))
          .resolve("hapi-fhir-validator-igs")
          .resolve("url-" + UUID.randomUUID() + extension);
      Files.createDirectories(target.getParent());
      Files.write(target, response.body());
      return target;
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("failed to fetch IG URL " + sourceUrl + ": " + e.getMessage());
    }
  }

  private static String guessExtension(String path) {
    String lower = (path == null ? "" : path).toLowerCase(Locale.ROOT);
    if (lower.endsWith(".tgz")) return ".tgz";
    if (lower.endsWith(".json")) return ".json";
    if (lower.endsWith(".xml")) return ".xml";
    return ".tgz";
  }

  public record ResolvedSources(List<String> values, List<Path> tempFiles) {}
}
