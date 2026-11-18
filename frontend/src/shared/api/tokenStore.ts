export interface Tokens {
  accessToken: string;
  refreshToken: string;
}

/*
 * The access token lives only in memory: a script injected into the page cannot find it in storage.
 * The refresh token survives a page reload in sessionStorage, which the browser clears when the tab is closed,
 * so closing the bank's tab logs the client out, as banks do.
 */
const REFRESH_TOKEN_KEY = 'taborovets.refreshToken';

let accessToken: string | null = null;
const sessionEndListeners = new Set<() => void>();

export function getAccessToken(): string | null {
  return accessToken;
}

export function getRefreshToken(): string | null {
  try {
    return sessionStorage.getItem(REFRESH_TOKEN_KEY);
  } catch {
    return null;
  }
}

export function saveTokens(tokens: Tokens): void {
  accessToken = tokens.accessToken;
  try {
    sessionStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
  } catch {
    // Without storage the session still works until the page is reloaded.
  }
}

export function clearTokens(): void {
  accessToken = null;
  try {
    sessionStorage.removeItem(REFRESH_TOKEN_KEY);
  } catch {
    // Nothing was stored.
  }
}

/** Called when the bank refuses to refresh the session, e.g. after a logout on another device. */
export function onSessionEnd(listener: () => void): () => void {
  sessionEndListeners.add(listener);
  return () => sessionEndListeners.delete(listener);
}

export function endSession(): void {
  clearTokens();
  sessionEndListeners.forEach((listener) => listener());
}
