package olympus.fhirvalidator.infrastructure;

import olympus.fhirvalidator.domain.ValidateOptions;
import org.hl7.fhir.validation.ValidationEngine;

final class EngineOptionMapper {
  private EngineOptionMapper() {}

  static void apply(ValidationEngine engine, ValidateOptions opts) {
    if (opts.terminologyServer != null && !opts.terminologyServer.isBlank() && !"n/a".equalsIgnoreCase(opts.terminologyServer)) {
      engine.setTerminologyServer(opts.terminologyServer);
    } else if ("n/a".equalsIgnoreCase(opts.terminologyServer)) {
      engine.setNoTerminologyServer(true);
    }
    if (opts.terminologyCache != null && !opts.terminologyCache.isBlank() && !"n/a".equalsIgnoreCase(opts.terminologyCache)) {
      engine.setTerminologyCachePath(opts.terminologyCache);
    } else if ("n/a".equalsIgnoreCase(opts.terminologyCache)) {
      engine.setTerminologyCachePath(null);
    }

    if (Boolean.TRUE.equals(opts.nativeSchema)) {
      engine.setNativeValidation(true);
    }
    if (Boolean.TRUE.equals(opts.checkReferences)) {
      engine.setCheckReferences(true);
    }
    if (Boolean.TRUE.equals(opts.igRecurse)) {
      engine.setIgRecursive(true);
    }
    if (opts.validationTimeoutMs != null && opts.validationTimeoutMs > 0) {
      engine.setTimeTracker(opts.validationTimeoutMs);
    }
    if (opts.maxValidationMessages != null && opts.maxValidationMessages > 0) {
      engine.setMaxErrors(opts.maxValidationMessages);
    }
  }
}
