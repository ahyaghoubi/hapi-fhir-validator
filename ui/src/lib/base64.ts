export function encodeUtf8ToBase64(value: string): string {
  return btoa(unescape(encodeURIComponent(value)))
}

export function decodeBase64ToUtf8(value: string): string {
  return decodeURIComponent(escape(atob(value)))
}
