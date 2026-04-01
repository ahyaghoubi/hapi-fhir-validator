package olympus.fhirvalidator.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class OpsResourceTest {
  @Test
  void capabilitiesEndpointWorks() {
    given()
        .when().get("/v1/capabilities")
        .then()
        .statusCode(200)
        .body("service", equalTo("hapi-fhir-validator"))
        .body("version", equalTo("v1"));
  }

  @Test
  void readyEndpointWorks() {
    given()
        .when().get("/v1/ready")
        .then()
        .statusCode(200)
        .body("ready", equalTo(true))
        .body("maxConcurrency", notNullValue());
  }

  @Test
  void configEndpointWorks() {
    given()
        .when().get("/v1/config")
        .then()
        .statusCode(200)
        .body("openApiPath", equalTo("/openapi"));
  }
}
