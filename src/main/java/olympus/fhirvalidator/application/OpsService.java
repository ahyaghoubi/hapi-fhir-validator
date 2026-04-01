package olympus.fhirvalidator.application;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import olympus.fhirvalidator.domain.CapabilitiesResponse;
import olympus.fhirvalidator.domain.ReadyResponse;
import olympus.fhirvalidator.domain.RuntimeConfigResponse;
import olympus.fhirvalidator.domain.WarmupRequest;
import olympus.fhirvalidator.domain.WarmupResponse;
import olympus.fhirvalidator.infrastructure.ValidatorEngine;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class OpsService {
  @Inject
  ValidatorEngine engine;

  @ConfigProperty(name = "quarkus.http.limits.max-body-size", defaultValue = "25M")
  String maxBodySize;

  @ConfigProperty(name = "validator.max-concurrency", defaultValue = "4")
  int maxConcurrency;

  public CapabilitiesResponse capabilities() {
    CapabilitiesResponse out = new CapabilitiesResponse();
    out.service = "hapi-fhir-validator";
    out.version = "v1";
    out.endpoints = List.of("/validate", "/v1/validate", "/v1/capabilities", "/v1/warmup", "/v1/ready", "/v1/config");
    out.supportedFhirVersions = List.of("3.0", "4.0", "4.0.1", "5.0");
    out.invariants = Map.of("output_style", "json", "request_payload", "resourceBase64 + opts", "valid_rule",
        "false when any issue.severity is error or fatal");
    out.options = Map.of("implementation_guides", "repeatable value list", "profiles", "repeatable value list",
        "bundle", "bundle_target and bundle_profile must be set together");
    return out;
  }

  public WarmupResponse warmup(WarmupRequest req) {
    String requestId = UUID.randomUUID().toString();
    long start = System.nanoTime();
    engine.warmup(req == null ? null : req.fhirVersion, req == null ? null : req.implementationGuides, requestId);
    WarmupResponse out = new WarmupResponse();
    out.warmed = true;
    out.requestId = requestId;
    out.durationMs = (System.nanoTime() - start) / 1_000_000L;
    out.message = "validator engine warmup completed";
    return out;
  }

  public ReadyResponse ready() {
    return engine.ready();
  }

  public RuntimeConfigResponse config() {
    RuntimeConfigResponse out = new RuntimeConfigResponse();
    out.maxConcurrency = maxConcurrency;
    out.maxRequestBodyBytes = parseSize(maxBodySize);
    out.prometheusEnabled = true;
    out.openApiPath = "/openapi";
    return out;
  }

  private static long parseSize(String value) {
    String v = value == null ? "" : value.trim().toUpperCase();
    if (v.endsWith("M")) return Long.parseLong(v.substring(0, v.length() - 1)) * 1024L * 1024L;
    if (v.endsWith("K")) return Long.parseLong(v.substring(0, v.length() - 1)) * 1024L;
    if (v.endsWith("G")) return Long.parseLong(v.substring(0, v.length() - 1)) * 1024L * 1024L * 1024L;
    return Long.parseLong(v);
  }
}
