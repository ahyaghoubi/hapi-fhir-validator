package olympus.fhirvalidator.infrastructure;

import olympus.fhirvalidator.domain.ValidateOptions;
import org.hl7.fhir.validation.ValidationEngine;

final class EngineOptionMapper {
  private EngineOptionMapper() {}

  static void apply(ValidationEngine engine, ValidateOptions opts) {
    // Keep option mapping conservative against org.hl7.fhir.validation API changes.
    if (Boolean.TRUE.equals(opts.nativeSchema)) {
      engine.setDoNative(true);
    }
  }
}
