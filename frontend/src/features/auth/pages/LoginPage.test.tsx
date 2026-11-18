import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { json, mockFetch, problem } from '@/test/fetchMock';
import { renderApp, TEST_USER } from '@/test/renderApp';

describe('LoginPage', () => {
  it('points at the empty fields instead of calling the bank', async () => {
    const fetchMock = mockFetch();
    renderApp('/login');

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
    renderApp('/login');

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
      ({ url }) => (url === '/users/me' ? json(TEST_USER) : undefined),
      ({ url }) => (url.startsWith('/notifications') ? json({ unread: 0 }) : undefined),
    );
    const router = renderApp('/cards');

    await userEvent.type(await screen.findByLabelText('Email'), 'ivan@example.com');
    await userEvent.type(screen.getByLabelText('Пароль'), 'correct-password');
    await userEvent.click(screen.getByRole('button', { name: 'Увійти' }));

    expect(await screen.findByRole('heading', { level: 1, name: 'Картки' })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/cards');
  });
});
