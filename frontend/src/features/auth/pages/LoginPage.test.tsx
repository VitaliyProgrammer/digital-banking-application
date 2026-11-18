import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter, RouterProvider } from 'react-router';
import { routes } from '@/app/routes';
import { json, mockFetch, problem } from '@/test/fetchMock';
import { AuthProvider } from '../model/AuthProvider';

const USER = {
  id: 'u1',
  email: 'ivan@example.com',
  firstName: 'Іван',
  lastName: 'Петренко',
  role: 'CLIENT',
  createdAt: '2026-11-01T10:00:00Z',
};

function openPage(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] });
  render(
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>,
  );
  return router;
}

describe('LoginPage', () => {
  it('points at the empty fields instead of calling the bank', async () => {
    const fetchMock = mockFetch();
    openPage('/login');

    await userEvent.click(screen.getByRole('button', { name: 'Увійти' }));

    expect(await screen.findAllByText('Заповніть це поле')).toHaveLength(2);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("shows the bank's own explanation of a refused login", async () => {
    mockFetch(({ url }) =>
      url === '/authentication/login'
        ? problem(401, 'invalid-credentials', 'Неправильний email або пароль')
        : undefined,
    );
    openPage('/login');

    await userEvent.type(screen.getByLabelText('Email'), 'ivan@example.com');
    await userEvent.type(screen.getByLabelText('Пароль'), 'wrong-password');
    await userEvent.click(screen.getByRole('button', { name: 'Увійти' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Неправильний email або пароль');
  });

  it('logs in and returns the client to the page they wanted to open', async () => {
    mockFetch(
      ({ url }) =>
        url === '/authentication/login'
          ? json({ accessToken: 'access', refreshToken: 'refresh', tokenType: 'Bearer', expiresIn: 900 })
          : undefined,
      ({ url }) => (url === '/users/me' ? json(USER) : undefined),
    );
    const router = openPage('/');

    await userEvent.type(await screen.findByLabelText('Email'), 'ivan@example.com');
    await userEvent.type(screen.getByLabelText('Пароль'), 'correct-password');
    await userEvent.click(screen.getByRole('button', { name: 'Увійти' }));

    expect(await screen.findByText('Ви увійшли як Іван Петренко')).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/');
  });
});
