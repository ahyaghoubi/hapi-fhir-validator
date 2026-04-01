package olympus.fhirvalidator.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import olympus.fhirvalidator.domain.ValueRow;
import org.junit.jupiter.api.Test;

class IgSourceResolverTest {
  @Test
  void resolvesAndDownloadsUrlIgs() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/package.tgz", exchange -> {
      byte[] body = "fake-ig".getBytes();
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    try {
      IgUploadStore uploadStore = new IgUploadStore();
      uploadStore.uploadDirOverride = Files.createTempDirectory("ig-upload-test");
      IgSourceResolver resolver = new IgSourceResolver();
      resolver.uploadStore = uploadStore;
      resolver.urlTimeoutMs = 5000L;

      ValueRow row = new ValueRow();
      row.value = "http://localhost:" + server.getAddress().getPort() + "/package.tgz";
      IgSourceResolver.ResolvedSources resolved = resolver.resolve(List.of(row));

      assertEquals(1, resolved.values().size());
      Path downloaded = Path.of(resolved.values().get(0));
      assertTrue(Files.exists(downloaded));
      resolver.cleanup(resolved);
      assertFalse(Files.exists(downloaded));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void resolvesStagedReferences() throws IOException {
    Path dir = Files.createTempDirectory("ig-staged-test");
    Path staged = dir.resolve("abc-package.tgz");
    Files.writeString(staged, "x");

    IgUploadStore uploadStore = new IgUploadStore();
    uploadStore.uploadDirOverride = dir;
    IgSourceResolver resolver = new IgSourceResolver();
    resolver.uploadStore = uploadStore;

    ValueRow row = new ValueRow();
    row.value = "staged://abc-package.tgz";
    IgSourceResolver.ResolvedSources resolved = resolver.resolve(List.of(row));
    assertEquals(staged.toString(), resolved.values().get(0));
  }

  @Test
  void throwsForMissingStagedReference() {
    IgUploadStore uploadStore = new IgUploadStore();
    uploadStore.uploadDirOverride = Path.of(System.getProperty("java.io.tmpdir"), "ig-missing-test");
    IgSourceResolver resolver = new IgSourceResolver();
    resolver.uploadStore = uploadStore;

    ValueRow row = new ValueRow();
    row.value = "staged://does-not-exist.tgz";
    assertThrows(IllegalArgumentException.class, () -> resolver.resolve(List.of(row)));
  }
}
