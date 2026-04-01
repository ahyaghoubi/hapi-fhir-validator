package olympus.fhirvalidator.infrastructure;

import olympus.fhirvalidator.domain.ValidateOptions;
import olympus.fhirvalidator.domain.ValidateResult;
import olympus.fhirvalidator.domain.ReadyResponse;
import olympus.fhirvalidator.domain.ValueRow;
import java.util.List;

public interface ValidatorEngine {
  ValidateResult validate(byte[] resourceBytes, ValidateOptions opts, String requestId);
  void warmup(String fhirVersion, List<ValueRow> implementationGuides, String requestId);
  ReadyResponse ready();
}

