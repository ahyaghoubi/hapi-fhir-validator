package olympus.fhirvalidator.domain;

import java.util.List;
import java.util.Map;

public class CapabilitiesResponse {
  public String service;
  public String version;
  public List<String> endpoints;
  public List<String> supportedFhirVersions;
  public Map<String, String> invariants;
  public Map<String, String> options;
}
