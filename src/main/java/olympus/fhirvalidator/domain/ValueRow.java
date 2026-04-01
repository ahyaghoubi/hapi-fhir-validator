package olympus.fhirvalidator.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class ValueRow {
  @JsonProperty("value")
  @NotBlank(message = "value must not be blank")
  public String value;
}

