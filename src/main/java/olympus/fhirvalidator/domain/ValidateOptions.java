package olympus.fhirvalidator.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public class ValidateOptions {
  @JsonProperty("label")
  public String label;

  @JsonProperty(value = "fhir_version", required = true)
  @NotBlank(message = "fhir_version is required")
  public String fhirVersion;

  @JsonProperty(value = "source_format", required = true)
  @NotBlank(message = "source_format is required")
  @Pattern(regexp = "^(json|xml)$", flags = Pattern.Flag.CASE_INSENSITIVE, message = "source_format must be json or xml")
  public String sourceFormat; // json|xml

  @JsonProperty("implementation_guides")
  @Valid
  public List<ValueRow> implementationGuides;

  @JsonProperty("profiles")
  @Valid
  public List<ValueRow> profiles;

  @JsonProperty("bundle_target")
  public String bundleTarget;

  @JsonProperty("bundle_profile")
  public String bundleProfile;

  @JsonProperty("terminology_server")
  public String terminologyServer;

  @JsonProperty("terminology_cache")
  public String terminologyCache;

  @JsonProperty("output_style")
  public String outputStyle;

  @JsonProperty("severity_floor")
  public String severityFloor;

  @JsonProperty("best_practice")
  public String bestPractice;

  @JsonProperty("native_schema")
  public Boolean nativeSchema;

  @JsonProperty("check_references")
  public Boolean checkReferences;

  @JsonProperty("ig_recurse")
  public Boolean igRecurse;

  @JsonProperty("validation_timeout_ms")
  @Min(value = 1, message = "validation_timeout_ms must be >= 1")
  public Long validationTimeoutMs;

  @JsonProperty("max_validation_messages")
  @Min(value = 1, message = "max_validation_messages must be >= 1")
  public Integer maxValidationMessages;
}

