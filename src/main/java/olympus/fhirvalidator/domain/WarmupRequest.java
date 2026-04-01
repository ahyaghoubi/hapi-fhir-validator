package olympus.fhirvalidator.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import java.util.List;

public class WarmupRequest {
  @JsonProperty("fhir_version")
  public String fhirVersion;

  @JsonProperty("implementation_guides")
  @Valid
  public List<ValueRow> implementationGuides;
}
