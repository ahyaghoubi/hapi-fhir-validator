package olympus.fhirvalidator.domain;

public class RuntimeConfigResponse {
  public int maxConcurrency;
  public long maxRequestBodyBytes;
  public boolean prometheusEnabled;
  public String openApiPath;
}
