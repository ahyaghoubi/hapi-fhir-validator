package olympus.fhirvalidator.domain;

public class ValidateResult {
  public boolean valid;
  public byte[] outcomeJson;
  public int exitCode;
  public String stderr;
  public long durationMs;
  public String requestId;
}

