import { decodeBase64ToUtf8 } from './base64.ts'

export type OutcomeIssue = {
  severity?: string
  code?: string
  diagnostics?: string
  location?: string[]
}

export type ParsedOutcome = {
  resourceType?: string
  issue?: OutcomeIssue[]
}

export function parseOutcomeBase64(outcomeBase64?: string): ParsedOutcome | null {
  if (!outcomeBase64) return null
  try {
    return JSON.parse(decodeBase64ToUtf8(outcomeBase64)) as ParsedOutcome
  } catch {
    return null
  }
}
