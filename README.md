# HAPI FHIR Validator (HL7 `validator_cli` wrapper)

This directory contains:

1. **`validator_cli.jar`** — the standalone FHIR validator from [hapifhir/org.hl7.fhir.core](https://github.com/hapifhir/org.hl7.fhir.core) (not the separate [HAPI FHIR CLI](https://hapifhir.io/hapi-fhir/docs/tools/hapi_fhir_cli.html) tarball); downloaded automatically on startup when missing.
2. **Java 17 warm validator service** — Quarkus HTTP service that validates in-process and keeps caches warm.

Use it to validate FHIR **JSON** or **XML** (single resource or `Bundle`) against the base spec, optional **Implementation Guides** (`-ig`), and optional **profiles** (`-profile`).

---

## Contents

- [Requirements](#requirements)
- [Layout](#layout)
- [Workflow node configuration](#workflow-node-configuration)
- [Implementation guides (`-ig`)](#implementation-guides--ig)
- [Workflow ports](#workflow-ports)
- [REST API endpoints](#rest-api-endpoints)
- [Environment variables](#environment-variables)
- [OpenAPI / Swagger](#openapi--swagger)
- [Testing](#testing)
- [Limits and extensions](#limits-and-extensions)
- [References](#references)

---

## Requirements

| Component | Version / notes |
|-----------|-----------------|
| **Java** | **17+** for the warm service |
| **Network** | Optional after first startup; needed initially if `data/validator_cli.jar` is missing, and also for package download, terminology (`-tx`), or IG URLs unless everything is local/offline |

---

## Layout

```
hapi-fhir-validator/
├── README.md                 ← this file
├── data/
│   └── validator_cli.jar     ← auto-downloaded at startup if missing
├── pom.xml                   ← Maven project definition
├── src/                      ← service source code (API, app, domain, infra)
└── Dockerfile                ← container image build
```

---

## Workflow node configuration

These keys map to the service request model `ValidateOptions` (same JSON field names).

| # | Key | Type | Required | CLI | Description |
|---|-----|------|----------|-----|-------------|
| 0 | `label` | string | yes | — | UI label only; **not** passed to the JAR. |
| 1 | `fhir_version` | string | yes | `-version` | FHIR release, e.g. `4.0`, `4.0.1`, `5.0`. The JAR defaults to **5.0** if omitted at the CLI—always set explicitly for R4. |
| 2 | `source_format` | `json` \| `xml` | yes | — | File extension for the temp input (`input.json` / `input.xml`). |
| 3 | `implementation_guides` | `[{ "value": "..." }]` | no | `-ig` (repeat) | IG sources; see [Implementation guides](#implementation-guides--ig). |
| 4 | `profiles` | `[{ "value": "..." }]` | no | `-profile` (repeat) | Canonical profile URLs (StructureDefinition), like `meta.profile`. |
| 4b | `bundle_target` | string | no | `-bundle` (1st of 2) | Reserved for CLI parity; currently accepted but not applied by the in-process service validator. |
| 4c | `bundle_profile` | string | no | `-bundle` (2nd of 2) | Reserved for CLI parity; currently accepted but not applied by the in-process service validator. |
| 5 | `terminology_server` | string | no | `-tx` | Empty → JAR default (`http://tx.fhir.org`). Use `n/a` for no terminology server. |
| 6 | `terminology_cache` | string | no | `-txCache` | Cache directory, or `n/a` to disable (common in containers). |
| 7 | `output_style` | string | no | `-output-style` | Service behavior is standardized around OperationOutcome JSON output. |
| 8 | `severity_floor` | string | no | `-level` | Minimum issue level emitted: `hint`, `warning`, `error`. |
| 9 | `best_practice` | string | no | `-best-practice` | `ignore`, `hint`, `warning`, `error`. |
| 10 | `native_schema` | bool | no | `-native` | Add native schema validation (XML/JSON Schema, ShEx) where supported. |
| 11 | `check_references` | bool | no | `-check-references` | Validate referenced resources when possible. |
| 12 | `ig_recurse` | bool | no | `-recurse` | Recurse into subfolders when `-ig` is a directory. |
| 13 | `validation_timeout_ms` | number | no | `-validation-timeout` | Wall-clock cap for a validation run. |
| 14 | `max_validation_messages` | number | no | `-max-validation-messages` | Stop after this many issues. |

`bundle_target` / `bundle_profile` are kept in the API contract for compatibility with CLI-style configs, but the current in-process engine path validates via profiles and does not execute focused `-bundle` validation.

The service returns **OperationOutcome** bytes (with **`output_style`** as configured).

---

## Implementation guides (`-ig`)

Each non-empty `implementation_guides[].value` becomes one **`-ig`** argument.

### Package registry (typical)

| Pattern | Example | Meaning |
|---------|---------|---------|
| `id#version` | `hl7.fhir.us.core#6.1.0` | Pinned NPM-style package. |
| `id#current` | `hl7.fhir.us.core#current` | Unreleased “current” stream (dev). |
| `id` only | `hl7.fhir.us.core` | Cache or fetch **latest** from the package server. |
| Version prefix | `[4.0]hl7.fhir.us.core#6.1.0` | Load in an R4 context (exact syntax: `java -jar … -help`). |

Examples: `hl7.fhir.uv.ips#2.0.0-ballot`, `hl7.fhir.uv.extensions.r4#5.1.0`. For national IGs, look up **package id** and **version** on [packages.fhir.org](https://packages.fhir.org).

### Local paths

| Example | Use |
|---------|-----|
| `/app/igs/my-ig` | Folder of conformance resources; set **`ig_recurse`** if nested. |
| `/data/package` | Unpacked FHIR NPM package (e.g. contains `package.json`). |
| `/tmp/MyProfile.json` | Single resource file. |
| `package.tgz` | Local `.tgz` package (same as notebook-style `-ig package.tgz`). |

### URL

Example: `https://build.fhir.org/ig/HL7/US-Core/` — requires network; must be loadable by your JAR version.

### Multiple IGs

```json
"implementation_guides": [
  { "value": "hl7.fhir.us.core#6.1.0" },
  { "value": "hl7.fhir.uv.extensions.r4#5.1.0" }
]
```

Equivalent: `-ig hl7.fhir.us.core#6.1.0 -ig hl7.fhir.uv.extensions.r4#5.1.0`.

---

## Workflow ports

| Port | Direction | Type | Meaning |
|------|-----------|------|---------|
| `resource` | input | bytes | FHIR JSON or XML to validate. |
| `valid` | output | bool | **`true`** iff parsed **OperationOutcome** has **no** `issue` with `severity` **`error`** or **`fatal`** (case-insensitive). Warnings/information do **not** flip `valid` to `false`. |
| `outcome` | output | bytes | Validator output (typically **OperationOutcome** JSON when `output_style` is `json`). |

---

## REST API endpoints

Primary API surface:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/v1/validate` | `POST` | Primary validation endpoint (base64 payload + options). |
| `/validate` | `POST` | Compatibility alias for existing callers. |
| `/v1/capabilities` | `GET` | Supported versions, options, invariants, and endpoint list. |
| `/v1/warmup` | `POST` | Preload validator context and optional IGs. |
| `/v1/ready` | `GET` | Readiness + concurrency status. |
| `/v1/config` | `GET` | Sanitized runtime configuration view. |

Validation request shape remains:

```json
{
  "opts": { "fhir_version": "4.0.1", "source_format": "json", "output_style": "json" },
  "resourceBase64": "eyJyZXNvdXJjZVR5cGUiOiJQYXRpZW50In0="
}
```

Validation response includes:
- `valid`, `outcomeBase64`, `exitCode`, `durationMs`, `requestId`
- and for errors: `errorCode`, `errorMessage`, `details`

---

## Environment variables

| Variable | Purpose |
|----------|---------|
| `VALIDATOR_MAX_CONCURRENCY` | Max parallel validations (if set by runtime env). |
| `VALIDATOR_CLI_JAR_PATH` | Optional override for local jar path (default: `data/validator_cli.jar`). |
| `VALIDATOR_CLI_JAR_DOWNLOAD_URL` | Optional override for jar download URL used when the local jar file is missing. |

---

## OpenAPI / Swagger

The Java service now exposes an OpenAPI spec and Swagger UI via Quarkus:

- OpenAPI JSON: `http://localhost:8082/openapi`
- Swagger UI: `http://localhost:8082/q/swagger-ui`
- Source spec file: `src/main/resources/META-INF/openapi.yaml`

---

## Warm validator service (HTTP, Java 17)

The warm service is intended for **high-throughput** validation where JVM startup and package/terminology initialization cost dominates.

**Important:** service mode supports **`output_style=json` only** to keep the `valid` rule well-defined (it is derived by parsing OperationOutcome JSON severities).

### Run locally (service)

From the project root (requires Java 17 and Maven):

```bash
mvn -q test
mvn -q quarkus:dev
```

Service defaults to `0.0.0.0:8082`.

### Call it with REST

```bash
curl -sS -X POST "http://localhost:8082/v1/validate" \
  -H "Content-Type: application/json" \
  -d '{
    "opts": { "fhir_version":"4.0.1", "source_format":"json", "output_style":"json" },
    "resourceBase64":"eyJyZXNvdXJjZVR5cGUiOiJQYXRpZW50In0="
  }'
```

**Containers:** ensure your application can reach the Java service URL from inside the container network.

### Operational endpoints examples

```bash
curl -sS "http://localhost:8082/v1/capabilities"
curl -sS "http://localhost:8082/v1/ready"
curl -sS "http://localhost:8082/v1/config"
curl -sS -X POST "http://localhost:8082/v1/warmup" -H "Content-Type: application/json" -d '{"fhir_version":"4.0.1"}'
```

---

## Testing

```bash
mvn -q test
```

## Limits and extensions

- **`-bundle` focused validation:** `bundle_target` / `bundle_profile` are currently compatibility fields and are not applied by the in-process validator path.
- This project is **not** the HAPI **`hapi-fhir-cli`** distribution.
- Service mode enforces `output_style=json` for deterministic `valid` semantics.

---

## What’s New

- Added a versioned REST surface (`/v1/*`) and kept `/validate` as compatibility alias.
- Added operational endpoints for capabilities, warmup, readiness, and runtime config.
- Added structured API errors with `requestId` and `details`.
- Java service docs are REST-first and aligned to the Quarkus service in this repository.
- Added startup bootstrap that downloads `validator_cli.jar` to `data/validator_cli.jar` when missing, and ignores it in Git.
- Updated service integration for `org.hl7.fhir.validation` 6.3.0 API compatibility.

---

## References

- [org.hl7.fhir.core releases](https://github.com/hapifhir/org.hl7.fhir.core/releases) (download `validator_cli.jar`)
- [Using the FHIR Validator](https://confluence.hl7.org/display/FHIR/Using+the+FHIR+Validator) (Confluence)
- [FHIR package registry](https://packages.fhir.org)

---

## Example `config.json`

```json
{
  "label": "US Core Patient check",
  "fhir_version": "4.0.1",
  "source_format": "json",
  "implementation_guides": [
    { "value": "hl7.fhir.us.core#6.1.0" }
  ],
  "profiles": [
    { "value": "http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient" }
  ],
  "terminology_server": "n/a",
  "terminology_cache": "n/a",
  "output_style": "json"
}
```

### NW Genomics message bundle (CLI parity fields)

Point **`implementation_guides`** at an unpacked IG root (folder with `package.json`, e.g. Desktop **`package`** after extracting the NPM package), or at a **`.tgz`**. Keep **`bundle_target`** / **`bundle_profile`** only if you need request-contract compatibility; current in-process validation is profile-based.

```json
{
  "label": "NW Genomics R01 DiagnosticReport in message bundle",
  "fhir_version": "4.0.1",
  "source_format": "json",
  "implementation_guides": [
    { "value": "/absolute/path/to/unpacked-ig-root" }
  ],
  "terminology_server": "n/a",
  "terminology_cache": "n/a",
  "output_style": "json",
  "bundle_target": "DiagnosticReport:0",
  "bundle_profile": "https://fhir.nwgenomics.nhs.uk/StructureDefinition/DiagnosticReport"
}
```

Equivalent notebook-style Java command (input path and `-ig` differ on your machine):

`java -jar validator_cli.jar Output/FHIR/R01/LRI-GeneVariant-3.txt.json -version 4.0.1 -ig package.tgz -bundle DiagnosticReport:0 https://fhir.nwgenomics.nhs.uk/StructureDefinition/DiagnosticReport -tx n/a`
