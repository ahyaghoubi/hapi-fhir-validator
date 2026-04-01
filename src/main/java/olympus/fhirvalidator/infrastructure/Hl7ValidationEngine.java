package olympus.fhirvalidator.infrastructure;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import org.hl7.fhir.r5.elementmodel.Manager;
import org.hl7.fhir.r5.formats.JsonParser;
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
  private final Object engineLock = new Object();
  private volatile ValidationEngine sharedEngine;
  private volatile String sharedEngineVersion;
  private volatile boolean engineWarm;
  @Inject
  IgSourceResolver igSourceResolver;

  @ConfigProperty(name = "validator.max-concurrency", defaultValue = "4")
  int configuredMaxConcurrency;
  @ConfigProperty(name = "validator.startup.fhir-version", defaultValue = "4.0.1")
  String configuredStartupFhirVersion;

  @PostConstruct
  void init() {
    // Keep it simple: bound concurrent validations to avoid memory spikes.
    maxConcurrency = Math.max(1, configuredMaxConcurrency);
    permits = new Semaphore(maxConcurrency);
    String startupVersion = normalizedVersion(configuredStartupFhirVersion);
    synchronized (engineLock) {
      sharedEngine = buildEngine(startupVersion);
      sharedEngineVersion = startupVersion;
      engineWarm = true;
    }
    log.info("validator engine initialized at startup version={}", startupVersion);
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
      synchronized (engineLock) {
        ValidationEngine engine = getOrRebuildEngine(opts.fhirVersion);
        IgSourceResolver.ResolvedSources resolvedIgs = igSourceResolver.resolve(opts.implementationGuides);
        try {
          // Load IGs (packages, folders, tgz, staged uploads, URL-downloaded temp files).
          for (String ig : resolvedIgs.values()) {
            boolean recursive = Boolean.TRUE.equals(opts.igRecurse);
            try {
              engine.getIgLoader().loadIg(engine.getIgs(), engine.getBinaries(), ig, recursive);
            } catch (Exception e) {
              throw new IllegalArgumentException("failed to load implementation guide: " + ig + " (" + e.getMessage() + ")");
            }
          }

          // Profiles to validate against (canonical URLs)
          List<String> profiles = valueRows(opts.profiles);

          EngineOptionMapper.apply(engine, opts);

          // Validate.
          InputStream in = new ByteArrayInputStream(resourceBytes);
          Manager.FhirFormat format = toFhirFormat(opts.sourceFormat);
          // severity floor and best-practice are CLI-centric; not all map cleanly. Keep defaults.

          // HL7 engine can return an OperationOutcome; we serialize to JSON and apply the same valid rule.
          org.hl7.fhir.r5.model.OperationOutcome outcome = engine.validate(format, in, profiles);
          byte[] outcomeJson = new JsonParser().composeString(outcome).getBytes(StandardCharsets.UTF_8);
          boolean valid = computeValidFromOutcomeJson(outcomeJson);

          ValidateResult r = new ValidateResult();
          r.valid = valid;
          r.outcomeJson = outcomeJson;
          r.exitCode = valid ? 0 : 1;
          r.stderr = "";
          r.durationMs = (System.nanoTime() - start) / 1_000_000L;
          r.requestId = requestId;
          return r;
        } finally {
          igSourceResolver.cleanup(resolvedIgs);
        }
      }
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

  @Override
  public void warmup(String fhirVersion, List<ValueRow> implementationGuides, String requestId) {
    String version = normalizedVersion(fhirVersion);
    try {
      synchronized (engineLock) {
        ValidationEngine engine = getOrRebuildEngine(version);
        IgSourceResolver.ResolvedSources resolvedIgs = igSourceResolver.resolve(implementationGuides);
        try {
          for (String ig : resolvedIgs.values()) {
            try {
              engine.getIgLoader().loadIg(engine.getIgs(), engine.getBinaries(), ig, true);
            } catch (Exception e) {
              throw new IllegalArgumentException("failed to warmup implementation guide: " + ig + " (" + e.getMessage() + ")");
            }
          }
        } finally {
          igSourceResolver.cleanup(resolvedIgs);
        }
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
    out.warmInMemory = engineWarm;
    out.warmVersion = sharedEngineVersion;
    out.status = (out.ready && engineWarm) ? "ready" : "initializing";
    return out;
  }

  private ValidationEngine getOrRebuildEngine(String requestedVersion) {
    String normalizedRequested = normalizedVersion(requestedVersion);
    if (sharedEngine == null || !normalizedRequested.equals(sharedEngineVersion)) {
      sharedEngine = buildEngine(normalizedRequested);
      sharedEngineVersion = normalizedRequested;
      engineWarm = true;
      log.info("validator engine rebuilt in-memory version={}", normalizedRequested);
    }
    return sharedEngine;
  }

  private static ValidationEngine buildEngine(String fhirVersion) {
    try {
      return new ValidationEngine.ValidationEngineBuilder()
          .withVersion(fhirVersion)
          .fromNothing();
    } catch (Exception e) {
      throw new IllegalArgumentException("failed to initialize validator engine version " + fhirVersion + ": " + e.getMessage());
    }
  }

  private static String normalizedVersion(String fhirVersion) {
    return (fhirVersion == null || fhirVersion.isBlank()) ? "4.0.1" : fhirVersion.trim();
  }

  private static Manager.FhirFormat toFhirFormat(String sourceFormat) {
    if (sourceFormat != null && "xml".equalsIgnoreCase(sourceFormat.trim())) {
      return Manager.FhirFormat.XML;
    }
    return Manager.FhirFormat.JSON;
  }
}

