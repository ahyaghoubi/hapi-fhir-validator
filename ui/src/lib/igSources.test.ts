import { describe, expect, it } from 'vitest'
import { buildImplementationGuides, buildProfiles } from './igSources.ts'

describe('igSources', () => {
  it('builds implementation guides from text and uploaded refs', () => {
    const out = buildImplementationGuides(
      ['hl7.fhir.us.core#6.1.0', '  '],
      ['staged://abc.tgz'],
    )
    expect(out).toEqual([
      { value: 'hl7.fhir.us.core#6.1.0' },
      { value: 'staged://abc.tgz' },
    ])
  })

  it('returns undefined when no profiles are provided', () => {
    const out = buildProfiles(['', '   '])
    expect(out).toBeUndefined()
  })
})
