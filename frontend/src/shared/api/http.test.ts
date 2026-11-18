import { json, mockFetch, problem } from '@/test/fetchMock';
import { ApiError } from './ApiError';
import { request } from './http';
import { getRefreshToken, onSessionEnd, saveTokens } from './tokenStore';

const ACCOUNTS = [{ id: 'a1' }];

describe('request', () => {
  beforeEach(() => {
    saveTokens({ accessToken: 'expired', refreshToken: 'refresh-1' });
  });

  it('refreshes an expired session once and repeats the call', async () => {
    const fetchMock = mockFetch(
      ({ url, headers }) =>
        url === '/accounts' && headers.get('Authorization') === 'Bearer expired'
          ? problem(401, 'unauthorized', 'Expired')
          : undefined,
      ({ url }) =>
        url === '/authentication/refresh'
          ? json({ accessToken: 'fresh', refreshToken: 'refresh-2' })
          : undefined,
      ({ url, headers }) =>
        url === '/accounts' && headers.get('Authorization') === 'Bearer fresh' ? json(ACCOUNTS) : undefined,
    );

    await expect(request('/accounts')).resolves.toEqual(ACCOUNTS);
    expect(getRefreshToken()).toBe('refresh-2');
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });

  it('refreshes only once when several calls expire at the same moment', async () => {
    const fetchMock = mockFetch(
      ({ url, headers }) =>
        url !== '/authentication/refresh' && headers.get('Authorization') === 'Bearer expired'
          ? problem(401, 'unauthorized', 'Expired')
          : undefined,
      ({ url }) =>
        url === '/authentication/refresh'
          ? json({ accessToken: 'fresh', refreshToken: 'refresh-2' })
          : undefined,
      () => json(ACCOUNTS),
    );

    await Promise.all([request('/accounts'), request('/cards'), request('/notifications')]);

    const refreshCalls = fetchMock.mock.calls.filter(([url]) => url === '/authentication/refresh');
    expect(refreshCalls).toHaveLength(1);
  });

  it('ends the session when the bank refuses to refresh it', async () => {
    const sessionEnded = vi.fn();
    onSessionEnd(sessionEnded);
    mockFetch(
      ({ url }) =>
        url === '/authentication/refresh' ? problem(401, 'invalid-token', 'Session revoked') : undefined,
      () => problem(401, 'unauthorized', 'Expired'),
    );

    const call = request('/accounts');

    await expect(call).rejects.toBeInstanceOf(ApiError);
    expect(sessionEnded).toHaveBeenCalledOnce();
    expect(getRefreshToken()).toBeNull();
  });

  it('sends the chosen language, so the bank answers errors in it', async () => {
    const fetchMock = mockFetch(() => json(ACCOUNTS));

    await request('/accounts');

    const init = fetchMock.mock.lastCall?.[1];
    expect(new Headers(init?.headers).get('Accept-Language')).toBe('uk');
  });
});
