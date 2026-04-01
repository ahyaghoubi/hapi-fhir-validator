package olympus.fhirvalidator.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ValidateResourceContractTest {
  @Test
  void validateAliasReturnsStructuredErrorForInvalidRequest() {
    given()
        .contentType("application/json")
        .body("{\"resourceBase64\":\"\",\"opts\":{\"fhir_version\":\"4.0.1\",\"source_format\":\"json\"}}")
        .when().post("/validate")
        .then()
        .statusCode(400)
        .body("errorCode", equalTo("validation_error"))
        .body("requestId", notNullValue());
  }

  @Test
  void validateV1ReturnsStructuredErrorForInvalidRequest() {
    given()
        .contentType("application/json")
        .body("{\"resourceBase64\":\"\",\"opts\":{\"fhir_version\":\"4.0.1\",\"source_format\":\"json\"}}")
        .when().post("/v1/validate")
        .then()
        .statusCode(400)
        .body("errorCode", equalTo("validation_error"))
        .body("requestId", notNullValue());
  }
}
