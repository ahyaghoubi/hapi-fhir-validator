import type { ValueRow } from '../types/api.ts'

function toRows(values: string[]): ValueRow[] | undefined {
  const rows = values
    .map((value) => value.trim())
    .filter(Boolean)
    .map((value) => ({ value }))
  return rows.length ? rows : undefined
}

export function buildImplementationGuides(
  textInputs: string[],
  uploadedReferences: string[],
): ValueRow[] | undefined {
  return toRows([...textInputs, ...uploadedReferences])
}

export function buildProfiles(textInputs: string[]): ValueRow[] | undefined {
  return toRows(textInputs)
}
