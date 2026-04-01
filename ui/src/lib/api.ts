import { API_BASE_URL } from './config.ts'
import type {
  CapabilitiesResponse,
  IgUploadResponse,
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
  const isFormData = typeof FormData !== 'undefined' && init?.body instanceof FormData
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: isFormData ? init?.headers : { 'Content-Type': 'application/json', ...init?.headers },
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
  uploadIg(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request<IgUploadResponse>('/v1/igs/upload', {
      method: 'POST',
      body: formData,
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
