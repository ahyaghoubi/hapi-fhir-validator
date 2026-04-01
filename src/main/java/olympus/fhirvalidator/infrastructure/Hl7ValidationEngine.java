package olympus.fhirvalidator.infrastructure;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import org.hl7.fhir.validation.ValidationEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import olympus.fhirvalidator.domain.ReadyResponse;
import olympus.fhirvalidator.domain.ValidateOptions;
import olympus.fhirvalidator.domain.ValidateResult;
import olympus.fhirvalidator.domain.ValueRow;
import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class Hl7ValidationEngine implements ValidatorEngine {
  private static final Logger log = LoggerFactory.getLogger(Hl7ValidationEngine.class);

  private Semaphore permits;
  private int maxConcurrency;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @ConfigProperty(name = "validator.max-concurrency", defaultValue = "4")
  int configuredMaxConcurrency;

  @PostConstruct
  void init() {
    // Keep it simple: bound concurrent validations to avoid memory spikes.
    maxConcurrency = Math.max(1, configuredMaxConcurrency);
    permits = new Semaphore(maxConcurrency);
  }

  @Override
  public ValidateResult validate(byte[] resourceBytes, ValidateOptions opts, String requestId) {
    Objects.requireNonNull(resourceBytes, "resourceBytes");
    Objects.requireNonNull(opts, "opts");

    try {
      permits.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalArgumentException("interrupted");
    }
    try {
      return doValidate(resourceBytes, opts, requestId);
    } finally {
      permits.release();
    }
  }

  private ValidateResult doValidate(byte[] resourceBytes, ValidateOptions opts, String requestId) {
    long start = System.nanoTime();

    // NOTE: The HL7 validator uses an R5 internal model but validates R2-R5; fhirVersion selects context.
    // We keep a per-request engine for safety; caches are controlled by the HL7 libraries via system props/env.
    try {
      ValidationEngine engine = new ValidationEngine(opts.fhirVersion);

      // Load IGs (packages, folders, tgz, etc.)
      for (String ig : valueRows(opts.implementationGuides)) {
        engine.loadIg(ig, true);
      }

      // Profiles to validate against (canonical URLs)
      List<String> profiles = valueRows(opts.profiles);

      // Bundle focus validation (message bundle)
      String bundleTarget = emptyToNull(opts.bundleTarget);
      String bundleProfile = emptyToNull(opts.bundleProfile);

      EngineOptionMapper.apply(engine, opts);

      // Validate.
      InputStream in = new ByteArrayInputStream(resourceBytes);
      // severity floor and best-practice are CLI-centric; not all map cleanly. Keep defaults.

      // HL7 engine can return an OperationOutcome; we serialize to JSON and apply the same valid rule.
      org.hl7.fhir.r5.model.OperationOutcome outcome;
      if (bundleTarget != null && bundleProfile != null) {
        outcome = engine.validateBundle(in, opts.sourceFormat, bundleTarget, bundleProfile, profiles);
      } else if (!profiles.isEmpty()) {
        outcome = engine.validate(in, opts.sourceFormat, profiles);
      } else {
        outcome = engine.validate(in, opts.sourceFormat);
      }

      byte[] outcomeJson = engine.getContext().newJsonParser().setPrettyPrint(false).encodeResourceToString(outcome).getBytes(StandardCharsets.UTF_8);
      boolean valid = computeValidFromOutcomeJson(outcomeJson);

      ValidateResult r = new ValidateResult();
      r.valid = valid;
      r.outcomeJson = outcomeJson;
      r.exitCode = valid ? 0 : 1;
      r.stderr = "";
      r.durationMs = (System.nanoTime() - start) / 1_000_000L;
      r.requestId = requestId;
      return r;
    } catch (Exception e) {
      log.warn("validation failed requestId={}", requestId, e);
      throw new IllegalArgumentException("validation failed: " + e.getMessage());
    } finally {
      // no-op
    }
  }

  private static List<String> valueRows(List<olympus.fhirvalidator.domain.ValueRow> rows) {
    List<String> out = new ArrayList<>();
    if (rows == null) return out;
    for (olympus.fhirvalidator.domain.ValueRow r : rows) {
      if (r == null || r.value == null) continue;
      String v = r.value.trim();
      if (!v.isEmpty()) out.add(v);
    }
    return out;
  }

  private static boolean computeValidFromOutcomeJson(byte[] outcomeJson) {
    // minimal parse (same semantics as Go): false if any issue.severity is error or fatal.
    try {
      var node = OBJECT_MAPPER.readTree(outcomeJson);
      var issues = node.get("issue");
      if (issues == null || !issues.isArray()) return true;
      for (var it : issues) {
        var sev = it.get("severity");
        if (sev == null) continue;
        var s = sev.asText("").trim().toLowerCase();
        if ("error".equals(s) || "fatal".equals(s)) return false;
      }
      return true;
    } catch (Exception e) {
      // If we can't parse, treat as failure.
      return false;
    }
  }

  private static String emptyToNull(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? null : t;
  }

  @Override
  public void warmup(String fhirVersion, List<ValueRow> implementationGuides, String requestId) {
    String version = (fhirVersion == null || fhirVersion.isBlank()) ? "4.0.1" : fhirVersion.trim();
    try {
      ValidationEngine engine = new ValidationEngine(version);
      for (String ig : valueRows(implementationGuides)) {
        engine.loadIg(ig, true);
      }
      log.info("warmup completed requestId={} version={}", requestId, version);
    } catch (Exception e) {
      throw new IllegalArgumentException("warmup failed: " + e.getMessage());
    }
  }

  @Override
  public ReadyResponse ready() {
    ReadyResponse out = new ReadyResponse();
    out.ready = permits != null;
    out.maxConcurrency = maxConcurrency;
    out.availablePermits = permits == null ? 0 : permits.availablePermits();
    out.terminologyMode = "runtime-configurable";
    out.status = out.ready ? "ready" : "initializing";
    return out;
  }
}

