package olympus.fhirvalidator.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadedIgResponse {
  @JsonProperty("requestId")
  public String requestId;

  @JsonProperty("reference")
  public String reference;

  @JsonProperty("filename")
  public String filename;
}
