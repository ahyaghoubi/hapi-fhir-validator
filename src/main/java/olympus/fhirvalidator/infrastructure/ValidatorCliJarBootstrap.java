package olympus.fhirvalidator.infrastructure;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

@ApplicationScoped
public class ValidatorCliJarBootstrap {
  private static final Logger log = LoggerFactory.getLogger(ValidatorCliJarBootstrap.class);

  @ConfigProperty(name = "validator.cli.jar.path", defaultValue = "data/validator_cli.jar")
  String configuredJarPath;

  @ConfigProperty(name = "validator.cli.jar.download-url", defaultValue = "https://github.com/hapifhir/org.hl7.fhir.core/releases/latest/download/validator_cli.jar")
  String configuredDownloadUrl;

  void onStart(@Observes StartupEvent ignored) {
    Path jarPath = resolveJarPath(configuredJarPath);
    if (Files.exists(jarPath)) {
      log.info("validator_cli.jar present at {}", jarPath);
      return;
    }

    try {
      downloadJar(jarPath, configuredDownloadUrl);
      log.info("validator_cli.jar downloaded to {}", jarPath);
    } catch (Exception e) {
      throw new IllegalStateException("failed to download validator_cli.jar from " + configuredDownloadUrl + " to " + jarPath + ": " + e.getMessage(), e);
    }
  }

  private static Path resolveJarPath(String configuredPath) {
    String value = configuredPath == null ? "" : configuredPath.trim();
    if (value.isEmpty()) {
      throw new IllegalArgumentException("validator.cli.jar.path must not be blank");
    }
    return Paths.get(value).toAbsolutePath().normalize();
  }

  private static void downloadJar(Path destination, String downloadUrl) throws IOException, InterruptedException {
    String url = downloadUrl == null ? "" : downloadUrl.trim();
    if (url.isEmpty()) {
      throw new IllegalArgumentException("validator.cli.jar.download-url must not be blank");
    }

    Path parent = destination.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }

    Path tempFile = parent == null
        ? Files.createTempFile("validator_cli", ".tmp")
        : Files.createTempFile(parent, "validator_cli", ".tmp");
    try {
      HttpClient client = HttpClient.newBuilder()
          .followRedirects(HttpClient.Redirect.ALWAYS)
          .connectTimeout(Duration.ofSeconds(20))
          .build();
      HttpRequest request = HttpRequest.newBuilder(URI.create(url))
          .GET()
          .timeout(Duration.ofMinutes(3))
          .header("User-Agent", "hapi-fhir-validator-service")
          .build();
      HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
      int status = response.statusCode();
      if (status < 200 || status >= 300) {
        throw new IOException("download failed with HTTP " + status);
      }

      try (InputStream body = response.body()) {
        Files.copy(body, tempFile, StandardCopyOption.REPLACE_EXISTING);
      }

      try {
        Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException ignored) {
        Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);
      }
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }
}
