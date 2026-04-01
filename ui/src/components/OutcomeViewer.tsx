import { parseOutcomeBase64 } from '../lib/outcome.ts'
import type { ValidateResponse } from '../types/api.ts'

type Props = {
  response: ValidateResponse | null
}

export function OutcomeViewer({ response }: Props) {
  if (!response) return null
  const parsed = parseOutcomeBase64(response.outcomeBase64)
  const issueCounts =
    parsed?.issue?.reduce<Record<string, number>>((acc, issue) => {
      const key = issue.severity ?? 'unknown'
      acc[key] = (acc[key] ?? 0) + 1
      return acc
    }, {}) ?? {}

  return (
    <section className="panel">
      <h2>Validation Result</h2>
      <p>
        Valid: <strong>{String(response.valid ?? false)}</strong>
      </p>
      <p>Duration: {response.durationMs ?? 0} ms</p>
      <p>Request ID: {response.requestId ?? 'n/a'}</p>
      {response.errorMessage ? <p>Error: {response.errorMessage}</p> : null}
      <div className="grid two-col">
        <button
          type="button"
          onClick={() => void navigator.clipboard.writeText(JSON.stringify(response, null, 2))}
        >
          Copy response JSON
        </button>
        <button
          type="button"
          onClick={() => {
            const blob = new Blob([JSON.stringify(parsed ?? response, null, 2)], {
              type: 'application/json',
            })
            const url = URL.createObjectURL(blob)
            const a = document.createElement('a')
            a.href = url
            a.download = 'validation-outcome.json'
            a.click()
            URL.revokeObjectURL(url)
          }}
        >
          Download outcome
        </button>
      </div>
      {Object.keys(issueCounts).length ? (
        <p>
          {Object.entries(issueCounts)
            .map(([severity, count]) => `${severity}: ${count}`)
            .join(' | ')}
        </p>
      ) : null}
      {parsed?.issue?.length ? (
        <ul>
          {parsed.issue.map((issue, idx) => (
            <li key={`${issue.code ?? 'issue'}-${idx}`}>
              [{issue.severity}] {issue.code} - {issue.diagnostics}
            </li>
          ))}
        </ul>
      ) : (
        <p>No parsed OperationOutcome issues found.</p>
      )}
    </section>
  )
}
