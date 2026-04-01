package olympus.fhirvalidator.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Base64;
import java.util.UUID;
import olympus.fhirvalidator.domain.ValidateOptions;
import olympus.fhirvalidator.domain.ValidateRequest;
import olympus.fhirvalidator.domain.ValidateResponse;
import olympus.fhirvalidator.domain.ValidateResult;
import olympus.fhirvalidator.infrastructure.ValidatorEngine;

@ApplicationScoped
public class ValidationService {

  @Inject
  ValidatorEngine engine;

  public ValidateResponse validate(ValidateRequest req) {
    String requestId = UUID.randomUUID().toString();
    long start = System.nanoTime();

    if (req == null || req.opts == null) {
      throw new IllegalArgumentException("missing opts");
    }
    ValidateOptions opts = req.opts;
    ValidateOptionsNormalizer.normalizeAndValidate(opts);

    if (req.resourceBase64 == null || req.resourceBase64.isBlank()) {
      throw new IllegalArgumentException("missing resourceBase64");
    }

    byte[] resourceBytes;
    try {
      resourceBytes = Base64.getDecoder().decode(req.resourceBase64);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("resourceBase64 is not valid base64");
    }

    ValidateResult r = engine.validate(resourceBytes, opts, requestId);
    r.durationMs = (System.nanoTime() - start) / 1_000_000L;

    ValidateResponse out = new ValidateResponse();
    out.valid = r.valid;
    out.outcomeBase64 = Base64.getEncoder().encodeToString(r.outcomeJson);
    out.exitCode = r.exitCode;
    out.stderr = r.stderr;
    out.durationMs = r.durationMs;
    out.requestId = r.requestId;
    return out;
  }
}

