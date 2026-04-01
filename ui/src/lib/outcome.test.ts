import { describe, expect, it } from 'vitest'
import { parseOutcomeBase64 } from './outcome.ts'

describe('parseOutcomeBase64', () => {
  it('returns null for invalid base64 payload', () => {
    expect(parseOutcomeBase64('not-base64')).toBeNull()
  })
})
