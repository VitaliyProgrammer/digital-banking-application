import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { json, mockFetch } from '@/test/fetchMock';
import { renderApp, TEST_USER } from '@/test/renderApp';

const NOTIFICATIONS = {
  content: [
    {
      id: 'n1',
      eventType: 'payment.completed',
      title: 'Платіж проведено',
      body: 'Мобільний Один, 250,00 ₴',
      createdAt: '2026-11-18T09:42:00Z',
      readAt: null,
    },
  ],
  page: 0,
  size: 10,
  totalElements: 1,
  totalPages: 1,
};

function mockBank(unread: number) {
  return mockFetch(
    ({ url }) => (url === '/users/me' ? json(TEST_USER) : undefined),
    ({ url }) => (url === '/notifications/unread-count' ? json({ unread }) : undefined),
    ({ url }) => (url.startsWith('/notifications?') ? json(NOTIFICATIONS) : undefined),
    ({ url, method }) =>
      url === '/notifications/read-all' && method === 'POST'
        ? new Response(null, { status: 204 })
        : undefined,
  );
}

describe('AppLayout', () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it('greets the client by name in the vocative case until 17:00 in Kyiv', async () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date('2026-11-18T14:59:00Z'));
    mockBank(0);

    renderApp('/', { loggedIn: true });

    expect(await screen.findByRole('heading', { level: 1, name: 'Добрий день, Іване!' })).toBeInTheDocument();
  });

  it('wishes a good evening from 17:00 in Kyiv', async () => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date('2026-11-18T15:00:00Z'));
    mockBank(0);

    renderApp('/', { loggedIn: true });

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Добрий вечір, Іване!' }),
    ).toBeInTheDocument();
  });

  it('marks the open section in the menu and names it in the header', async () => {
    mockBank(0);

    renderApp('/savings', { loggedIn: true });

    const menu = await screen.findByRole('navigation', { name: 'Головне меню' });
    expect(within(menu).getByRole('link', { name: 'Скарбнички' })).toHaveAttribute('aria-current', 'page');
    expect(screen.getByRole('heading', { level: 1, name: 'Скарбнички' })).toBeInTheDocument();
  });

  it('shows the unread count on the bell and marks everything as read', async () => {
    const fetchMock = mockBank(2);
    renderApp('/', { loggedIn: true });

    await userEvent.click(await screen.findByRole('button', { name: 'Сповіщення, непрочитаних: 2' }));
    expect(await screen.findByText('Платіж проведено')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Прочитати всі' }));

    expect(fetchMock).toHaveBeenCalledWith(
      '/notifications/read-all',
      expect.objectContaining({ method: 'POST' }),
    );
  });
});
