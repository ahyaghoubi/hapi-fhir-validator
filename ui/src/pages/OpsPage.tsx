import { useMutation, useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../lib/api.ts'
import type { ValueRow } from '../types/api.ts'

export function OpsPage() {
  const [version, setVersion] = useState('4.0.1')
  const [ig, setIg] = useState('')

  const ready = useQuery({ queryKey: ['ready'], queryFn: api.ready })
  const capabilities = useQuery({ queryKey: ['capabilities'], queryFn: api.capabilities })
  const config = useQuery({ queryKey: ['config'], queryFn: api.config })
  const warmup = useMutation({
    mutationFn: (payload: { fhir_version: string; implementation_guides?: ValueRow[] }) =>
      api.warmup(payload),
  })

  return (
    <div className="grid">
      <section className="panel">
        <h2>Warmup</h2>
        <div className="grid two-col">
          <input value={version} onChange={(event) => setVersion(event.target.value)} />
          <input value={ig} onChange={(event) => setIg(event.target.value)} placeholder="Optional IG value" />
        </div>
        <button
          type="button"
          onClick={() =>
            warmup.mutate({
              fhir_version: version,
              implementation_guides: ig.trim() ? [{ value: ig.trim() }] : undefined,
            })
          }
        >
          Run warmup
        </button>
        {warmup.data ? <pre>{JSON.stringify(warmup.data, null, 2)}</pre> : null}
      </section>

      <section className="panel">
        <h2>Readiness</h2>
        {ready.isLoading ? <p>Loading...</p> : <pre>{JSON.stringify(ready.data, null, 2)}</pre>}
      </section>

      <section className="panel">
        <h2>Capabilities</h2>
        {capabilities.isLoading ? (
          <p>Loading...</p>
        ) : (
          <pre>{JSON.stringify(capabilities.data, null, 2)}</pre>
        )}
      </section>

      <section className="panel">
        <h2>Runtime Config</h2>
        {config.isLoading ? <p>Loading...</p> : <pre>{JSON.stringify(config.data, null, 2)}</pre>}
      </section>
    </div>
  )
}
