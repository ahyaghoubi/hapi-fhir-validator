import { useMutation } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { OptionsForm, type ValidateFormValues } from '../components/OptionsForm.tsx'
import { OutcomeViewer } from '../components/OutcomeViewer.tsx'
import { api } from '../lib/api.ts'
import { encodeUtf8ToBase64 } from '../lib/base64.ts'
import type { ValidateRequest, ValidateResponse, ValueRow } from '../types/api.ts'

function toValueRows(first: string, second: string): ValueRow[] | undefined {
  const values = [first, second]
    .map((value) => value.trim())
    .filter(Boolean)
    .map((value) => ({ value }))
  return values.length ? values : undefined
}

export function ValidatePage() {
  const [response, setResponse] = useState<ValidateResponse | null>(null)
  const [clientError, setClientError] = useState<string | null>(null)
  const { register, handleSubmit, setValue, formState } = useForm<ValidateFormValues>({
    defaultValues: {
      resourceText: '{\n  "resourceType": "Patient"\n}',
      opts: {
        fhir_version: '4.0.1',
        source_format: 'json',
        output_style: 'json',
        terminology_server: 'n/a',
        terminology_cache: 'n/a',
      },
      ig1: '',
      ig2: '',
      profile1: '',
      profile2: '',
    },
  })

  const mutation = useMutation({
    mutationFn: (payload: ValidateRequest) => api.validate(payload),
    onSuccess: (data) => {
      setClientError(null)
      setResponse(data)
    },
    onError: () => {
      setClientError('Validation request failed. Check API connectivity and payload options.')
    },
  })

  const onSubmit = handleSubmit((values) => {
    try {
      if (values.opts.source_format === 'json') {
        JSON.parse(values.resourceText)
      }
    } catch {
      setClientError('JSON source format selected, but resource payload is not valid JSON.')
      return
    }
    const timeout =
      typeof values.opts.validation_timeout_ms === 'number' &&
      Number.isFinite(values.opts.validation_timeout_ms) &&
      values.opts.validation_timeout_ms > 0
        ? values.opts.validation_timeout_ms
        : undefined
    const maxMessages =
      typeof values.opts.max_validation_messages === 'number' &&
      Number.isFinite(values.opts.max_validation_messages) &&
      values.opts.max_validation_messages > 0
        ? values.opts.max_validation_messages
        : undefined
    const payload: ValidateRequest = {
      resourceBase64: encodeUtf8ToBase64(values.resourceText),
      opts: {
        ...values.opts,
        output_style: 'json',
        validation_timeout_ms: timeout,
        max_validation_messages: maxMessages,
        implementation_guides: toValueRows(values.ig1, values.ig2),
        profiles: toValueRows(values.profile1, values.profile2),
      },
    }
    mutation.mutate(payload)
  })

  return (
    <div className="grid">
      <section className="panel">
        <h2>Resource Input</h2>
        <div className="field">
          <label htmlFor="resourceText">FHIR JSON/XML</label>
          <textarea
            id="resourceText"
            rows={14}
            {...register('resourceText', { required: 'Resource input is required.' })}
          />
        </div>
        <div className="field">
          <label htmlFor="file">Load from file</label>
          <input
            id="file"
            type="file"
            onChange={async (event) => {
              const file = event.target.files?.[0]
              if (!file) return
              setValue('resourceText', await file.text())
            }}
          />
        </div>
      </section>

      <OptionsForm register={register} errors={formState.errors} />

      <button type="button" onClick={() => void onSubmit()} disabled={mutation.isPending}>
        {mutation.isPending ? 'Validating...' : 'Validate'}
      </button>
      {clientError ? <p role="alert">{clientError}</p> : null}

      <OutcomeViewer response={response} />
    </div>
  )
}
