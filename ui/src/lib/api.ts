import { API_BASE_URL } from './config.ts'
import type {
  CapabilitiesResponse,
  ReadyResponse,
  RuntimeConfigResponse,
  ValidateRequest,
  ValidateResponse,
  WarmupRequest,
  WarmupResponse,
} from '../types/api.ts'

async function request<TResponse>(
  path: string,
  init?: RequestInit,
): Promise<TResponse> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  })
  const body = (await response.json()) as TResponse
  if (!response.ok) {
    throw body
  }
  return body
}

export const api = {
  validate(payload: ValidateRequest) {
    return request<ValidateResponse>('/v1/validate', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  warmup(payload: WarmupRequest) {
    return request<WarmupResponse>('/v1/warmup', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
  ready() {
    return request<ReadyResponse>('/v1/ready')
  },
  capabilities() {
    return request<CapabilitiesResponse>('/v1/capabilities')
  },
  config() {
    return request<RuntimeConfigResponse>('/v1/config')
  },
}
