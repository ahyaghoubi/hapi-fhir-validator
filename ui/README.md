# FHIR Validator UI

Frontend application for the Quarkus validator service in this repository.

## Features

- Full validation workflow against `POST /v1/validate`
- Complete `ValidateOptions` coverage (including compatibility bundle fields)
- Implementation guide sources from text input (package id/path/URL) and uploaded IG files
- Base64 payload encoding and OperationOutcome decoding
- Operational views for `ready`, `capabilities`, `config`, and `warmup`
- Typed API contract generated from backend OpenAPI

## Prerequisites

- Node.js 20+
- Running backend API on `http://localhost:8082`

## Run Locally

```bash
cd ui
npm install --cache .npm-cache
npm run gen:types
npm run dev
```

Vite runs on `http://localhost:5173`.

## Scripts

- `npm run dev` - start development server
- `npm run build` - type-check and build production assets
- `npm run lint` - strict ESLint checks
- `npm run test` - run unit tests once
- `npm run test:watch` - watch mode tests
- `npm run gen:types` - regenerate types from `../src/main/resources/META-INF/openapi.yaml`

## Configuration

- `VITE_API_BASE_URL` (optional): API base URL prefix.
  - Default is empty string, so browser calls `/v1/*` on same origin.
  - For split-host deployments, set this value at build time.

## IG Inputs

- Use "Implementation guides" text inputs for package ids, local paths visible to backend, or URLs.
- Use the IG file picker for `.tgz`, `.json`, or `.xml`; files are uploaded to `POST /v1/igs/upload`.
- Uploaded files return `staged://...` references and are automatically included in `implementation_guides` for validate requests.

## Directory Notes

- `src/pages/ValidatePage.tsx` - resource input + options + submit
- `src/components/OptionsForm.tsx` - full option form
- `src/components/OutcomeViewer.tsx` - decoded outcome and issue summary
- `src/pages/OpsPage.tsx` - warmup/readiness/capabilities/config
- `src/types/openapi.ts` - generated OpenAPI types
