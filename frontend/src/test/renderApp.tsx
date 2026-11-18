import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { routes } from '@/app/routes';
import { AuthProvider } from '@/features/auth/model/AuthProvider';
import { saveTokens } from '@/shared/api/tokenStore';

export const TEST_USER = {
  id: 'u1',
  email: 'ivan@example.com',
  firstName: 'Іван',
  lastName: 'Петренко',
  role: 'CLIENT',
  createdAt: '2026-11-01T10:00:00Z',
};

/** Opens the whole application at a path, as the browser would, with a fresh cache for every test. */
export function renderApp(path: string, { loggedIn = false } = {}) {
  if (loggedIn) {
    saveTokens({ accessToken: 'access', refreshToken: 'refresh' });
  }

  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const router = createMemoryRouter(routes, { initialEntries: [path] });

  render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>,
  );

  return router;
}
