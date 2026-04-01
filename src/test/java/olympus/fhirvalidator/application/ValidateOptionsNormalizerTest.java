package olympus.fhirvalidator.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import olympus.fhirvalidator.domain.ValidateOptions;
import org.junit.jupiter.api.Test;

class ValidateOptionsNormalizerTest {
  @Test
  void defaultsOutputStyleToJson() {
    ValidateOptions opts = new ValidateOptions();
    opts.fhirVersion = "4.0.1";
    opts.sourceFormat = "JSON";

    ValidateOptionsNormalizer.normalizeAndValidate(opts);

    assertEquals("json", opts.sourceFormat);
    assertEquals("json", opts.outputStyle);
  }

  @Test
  void rejectsPartialBundlePair() {
    ValidateOptions opts = new ValidateOptions();
    opts.fhirVersion = "4.0.1";
    opts.sourceFormat = "json";
    opts.bundleTarget = "Patient:0";

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> ValidateOptionsNormalizer.normalizeAndValidate(opts));
    assertEquals("bundle_target and bundle_profile must both be set or both omitted", ex.getMessage());
  }
}
