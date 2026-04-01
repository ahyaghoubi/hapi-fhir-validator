import { describe, expect, it } from 'vitest'
import { decodeBase64ToUtf8, encodeUtf8ToBase64 } from './base64.ts'

describe('base64 utilities', () => {
  it('round-trips utf8 text', () => {
    const input = '{"resourceType":"Patient","name":[{"text":"Jane Doe"}]}'
    const encoded = encodeUtf8ToBase64(input)
    expect(decodeBase64ToUtf8(encoded)).toBe(input)
  })
})
