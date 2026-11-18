import { vi } from 'vitest';

type Route = (request: {
  url: string;
  method: string;
  body: unknown;
  headers: Headers;
}) => Response | undefined;

/** Replaces fetch with a fake bank: each route answers the requests it recognises, in order. */
export function mockFetch(...routes: Route[]) {
  const fetchMock = vi.fn(async (url: string, init: RequestInit = {}) => {
    const request = {
      url,
      method: init.method ?? 'GET',
      body: typeof init.body === 'string' ? (JSON.parse(init.body) as unknown) : undefined,
      headers: new Headers(init.headers),
    };
    for (const route of routes) {
      const response = route(request);
      if (response) {
        return response;
      }
    }
    throw new Error(`Unexpected request: ${request.method} ${url}`);
  });

  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

export function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

export function problem(status: number, code: string, detail: string): Response {
  return json({ status, code, detail }, status);
}
