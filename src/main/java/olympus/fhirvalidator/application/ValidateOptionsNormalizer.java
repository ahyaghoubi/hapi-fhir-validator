package olympus.fhirvalidator.application;

import olympus.fhirvalidator.domain.ValidateOptions;

public final class ValidateOptionsNormalizer {
  private ValidateOptionsNormalizer() {}

  public static void normalizeAndValidate(ValidateOptions o) {
    o.fhirVersion = trim(o.fhirVersion);
    o.sourceFormat = lowerTrim(o.sourceFormat);
    o.bundleTarget = trim(o.bundleTarget);
    o.bundleProfile = trim(o.bundleProfile);
    o.terminologyServer = trim(o.terminologyServer);
    o.terminologyCache = trim(o.terminologyCache);
    o.outputStyle = trim(o.outputStyle);
    o.severityFloor = trim(o.severityFloor);
    o.bestPractice = trim(o.bestPractice);

    if (o.fhirVersion == null || o.fhirVersion.isBlank()) {
      throw new IllegalArgumentException("fhir_version is required");
    }
    if (!"json".equals(o.sourceFormat) && !"xml".equals(o.sourceFormat)) {
      throw new IllegalArgumentException("source_format must be json or xml");
    }
    boolean hasBundleTarget = o.bundleTarget != null && !o.bundleTarget.isBlank();
    boolean hasBundleProfile = o.bundleProfile != null && !o.bundleProfile.isBlank();
    if (hasBundleTarget != hasBundleProfile) {
      throw new IllegalArgumentException("bundle_target and bundle_profile must both be set or both omitted");
    }

    if (o.outputStyle == null || o.outputStyle.isBlank()) {
      o.outputStyle = "json";
    }
    if (!"json".equalsIgnoreCase(o.outputStyle)) {
      throw new IllegalArgumentException("service mode supports output_style=json only");
    }
  }

  private static String trim(String s) {
    if (s == null) return null;
    String t = s.trim();
    return t.isEmpty() ? "" : t;
  }

  private static String lowerTrim(String s) {
    if (s == null) return null;
    return s.trim().toLowerCase();
  }
}
