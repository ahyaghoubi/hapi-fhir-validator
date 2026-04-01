package olympus.fhirvalidator.domain;

public class ReadyResponse {
  public boolean ready;
  public int maxConcurrency;
  public int availablePermits;
  public String terminologyMode;
  public String status;
}
