package olympus.fhirvalidator.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ValidateRequest {
  @JsonProperty(value = "opts", required = true)
  @NotNull(message = "opts is required")
  @Valid
  public ValidateOptions opts;

  @JsonProperty(value = "resourceBase64", required = true)
  @NotBlank(message = "resourceBase64 is required")
  public String resourceBase64;
}

