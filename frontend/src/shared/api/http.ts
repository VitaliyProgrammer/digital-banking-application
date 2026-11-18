import { currentLanguage } from '@/shared/i18n/i18n';
import { ApiError } from './ApiError';
import { endSession, getAccessToken, getRefreshToken, saveTokens, type Tokens } from './tokenStore';

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE';

interface RequestOptions {
  method?: HttpMethod;
  body?: unknown;
  /** false for the calls made before logging in: login, registration, password reset. */
  authenticated?: boolean;
  signal?: AbortSignal;
}

let refreshInFlight: Promise<boolean> | null = null;

/**
 * Calls the bank's API. An expired access token is refreshed once and the call is repeated,
 * so the 15-minute token lifetime never interrupts the client.
 */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { authenticated = true } = options;

  if (authenticated && !getAccessToken() && getRefreshToken()) {
    await refreshSession();
  }

  let response = await send(path, options);

  if (response.status === 401 && authenticated && getRefreshToken()) {
    if (await refreshSession()) {
      response = await send(path, options);
    }
  }

  if (response.status === 401 && authenticated) {
    endSession();
  }

  if (!response.ok) {
    throw await ApiError.fromResponse(response);
  }

  return response.status === 204 ? (undefined as T) : ((await response.json()) as T);
}

async function send(path: string, { method = 'GET', body, authenticated = true, signal }: RequestOptions) {
  const headers = new Headers({ Accept: 'application/json', 'Accept-Language': currentLanguage() });
  const accessToken = getAccessToken();

  if (body !== undefined) {
    headers.set('Content-Type', 'application/json');
  }
  if (authenticated && accessToken) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }

  try {
    return await fetch(path, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error;
    }
    throw ApiError.network();
  }
}

/**
 * Several calls can fail with 401 at the same moment. They all wait for one refresh: the bank rotates
 * refresh tokens, so a second refresh with the same token would look like theft and end every session.
 */
function refreshSession(): Promise<boolean> {
  refreshInFlight ??= refreshTokens().finally(() => {
    refreshInFlight = null;
  });
  return refreshInFlight;
}

async function refreshTokens(): Promise<boolean> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    return false;
  }

  const response = await send('/authentication/refresh', {
    method: 'POST',
    body: { refreshToken },
    authenticated: false,
  });

  if (!response.ok) {
    return false;
  }

  saveTokens((await response.json()) as Tokens);
  return true;
}
