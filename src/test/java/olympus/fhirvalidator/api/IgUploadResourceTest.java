package olympus.fhirvalidator.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.startsWith;

import io.quarkus.test.junit.QuarkusTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

@QuarkusTest
class IgUploadResourceTest {
  @Test
  void uploadEndpointReturnsStagedReference() {
    given()
        .multiPart("file", "package.tgz", "fake-package".getBytes(StandardCharsets.UTF_8))
        .when().post("/v1/igs/upload")
        .then()
        .statusCode(200)
        .body("reference", startsWith("staged://"));
  }
}
