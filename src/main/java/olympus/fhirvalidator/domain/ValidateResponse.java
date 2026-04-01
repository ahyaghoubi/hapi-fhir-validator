package olympus.fhirvalidator.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidateResponse {
  @JsonProperty("valid")
  public Boolean valid;

  @JsonProperty("outcomeBase64")
  public String outcomeBase64;

  @JsonProperty("exitCode")
  public Integer exitCode;

  @JsonProperty("stderr")
  public String stderr;

  @JsonProperty("durationMs")
  public Long durationMs;

  @JsonProperty("requestId")
  public String requestId;

  @JsonProperty("errorCode")
  public String errorCode;

  @JsonProperty("errorMessage")
  public String errorMessage;

  @JsonProperty("details")
  public Map<String, Object> details;

  public static ValidateResponse error(String code, String message) {
    ValidateResponse r = new ValidateResponse();
    r.errorCode = code;
    r.errorMessage = message;
    return r;
  }

  public static ValidateResponse error(String code, String message, String requestId, Map<String, Object> details) {
    ValidateResponse r = error(code, message);
    r.requestId = requestId;
    r.details = details;
    return r;
  }
}

