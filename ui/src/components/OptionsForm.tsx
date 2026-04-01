import type { FieldErrors, UseFormRegister } from 'react-hook-form'
import type { ValidateOptions } from '../types/api.ts'

export type ValidateFormValues = {
  resourceText: string
  opts: ValidateOptions
  ig1: string
  ig2: string
  profile1: string
  profile2: string
}

type Props = {
  register: UseFormRegister<ValidateFormValues>
  errors: FieldErrors<ValidateFormValues>
  onIgFilesSelected: (files: FileList | null) => void
  uploadedIgReferences: string[]
}

export function OptionsForm({ register, errors, onIgFilesSelected, uploadedIgReferences }: Props) {
  return (
    <section className="panel">
      <h2>Validation Options</h2>
      <div className="grid two-col">
        <div className="field">
          <label htmlFor="fhir_version">FHIR version</label>
          <input id="fhir_version" {...register('opts.fhir_version')} placeholder="4.0.1" />
        </div>
        <div className="field">
          <label htmlFor="source_format">Source format</label>
          <select id="source_format" {...register('opts.source_format')}>
            <option value="json">json</option>
            <option value="xml">xml</option>
          </select>
        </div>
      </div>
      <div className="field">
        <label htmlFor="ig1">Implementation guides (optional)</label>
        <input id="ig1" {...register('ig1')} placeholder="hl7.fhir.us.core#6.1.0" />
        <input {...register('ig2')} placeholder="second IG (optional)" />
        <label htmlFor="ig-files">Upload IG file(s) (.tgz/.json/.xml)</label>
        <input
          id="ig-files"
          type="file"
          multiple
          accept=".tgz,.json,.xml"
          onChange={(event) => onIgFilesSelected(event.target.files)}
        />
        {uploadedIgReferences.length ? (
          <small>Uploaded IG refs: {uploadedIgReferences.join(', ')}</small>
        ) : null}
      </div>
      <div className="field">
        <label htmlFor="profile1">Profiles (optional)</label>
        <input id="profile1" {...register('profile1')} placeholder="canonical profile URL" />
        <input {...register('profile2')} placeholder="second profile (optional)" />
      </div>
      <div className="grid two-col">
        <div className="field">
          <label htmlFor="terminology_server">Terminology server</label>
          <input id="terminology_server" {...register('opts.terminology_server')} placeholder="n/a" />
        </div>
        <div className="field">
          <label htmlFor="terminology_cache">Terminology cache</label>
          <input id="terminology_cache" {...register('opts.terminology_cache')} placeholder="n/a" />
        </div>
      </div>
      <div className="grid two-col">
        <div className="field">
          <label htmlFor="bundle_target">Bundle target (compatibility)</label>
          <input id="bundle_target" {...register('opts.bundle_target')} placeholder="DiagnosticReport:0" />
        </div>
        <div className="field">
          <label htmlFor="bundle_profile">Bundle profile (compatibility)</label>
          <input id="bundle_profile" {...register('opts.bundle_profile')} placeholder="https://..." />
        </div>
      </div>
      <div className="grid two-col">
        <div className="field">
          <label htmlFor="severity_floor">Severity floor</label>
          <select id="severity_floor" {...register('opts.severity_floor')}>
            <option value="">(default)</option>
            <option value="hint">hint</option>
            <option value="warning">warning</option>
            <option value="error">error</option>
          </select>
        </div>
        <div className="field">
          <label htmlFor="best_practice">Best practice</label>
          <select id="best_practice" {...register('opts.best_practice')}>
            <option value="">(default)</option>
            <option value="ignore">ignore</option>
            <option value="hint">hint</option>
            <option value="warning">warning</option>
            <option value="error">error</option>
          </select>
        </div>
      </div>
      <div className="grid two-col">
        <div className="field">
          <label htmlFor="timeout">Validation timeout (ms)</label>
          <input id="timeout" type="number" {...register('opts.validation_timeout_ms', { valueAsNumber: true })} />
        </div>
        <div className="field">
          <label htmlFor="max_msgs">Max validation messages</label>
          <input id="max_msgs" type="number" {...register('opts.max_validation_messages', { valueAsNumber: true })} />
        </div>
      </div>
      <div className="field">
        <label>
          <input type="checkbox" {...register('opts.native_schema')} /> Native schema
        </label>
        <label>
          <input type="checkbox" {...register('opts.check_references')} /> Check references
        </label>
        <label>
          <input type="checkbox" {...register('opts.ig_recurse')} /> IG recurse
        </label>
      </div>
      {errors.resourceText ? <p role="alert">{errors.resourceText.message}</p> : null}
    </section>
  )
}
