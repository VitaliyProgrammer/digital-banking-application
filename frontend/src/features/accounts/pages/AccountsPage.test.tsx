import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { json, mockFetch } from '@/test/fetchMock';
import { ACCOUNTS } from '@/test/homeFixtures';
import { renderApp, TEST_USER } from '@/test/renderApp';

function statementEntry(transactionId: string, createdAt: string, description: string) {
  return {
    transactionId,
    type: 'TRANSFER',
    direction: 'DEBIT',
    amount: 100,
    balanceAfter: 1140,
    description,
    createdAt,
  };
}

const USD_FIRST_PAGE = {
  content: [
    statementEntry('t3', '2026-11-17T10:00:00Z', 'Оплата хостингу'),
    statementEntry('t2', '2026-11-16T09:00:00Z', 'Переказ Олені'),
  ],
  page: 0,
  size: 20,
  totalElements: 3,
  totalPages: 2,
};

const USD_SECOND_PAGE = {
  content: [statementEntry('t1', '2026-11-02T09:00:00Z', 'Підписка на курси')],
  page: 1,
  size: 20,
  totalElements: 3,
  totalPages: 2,
};

const EMPTY_PAGE = { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 };

/** The layout around every section asks for the user and the bell. */
function layoutAnswer(url: string) {
  if (url === '/users/me') {
    return json(TEST_USER);
  }
  if (url === '/notifications/unread-count') {
    return json({ unread: 0 });
  }
  return undefined;
}

describe('AccountsPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date('2026-11-17T12:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('opens the account from the address and shows its statement by days, a page at a time', async () => {
    mockFetch(
      ({ url }) => layoutAnswer(url),
      ({ url }) => (url === '/accounts' ? json(ACCOUNTS) : undefined),
      ({ url }) =>
        url === '/accounts/acc-usd/transactions?page=0&size=20' ? json(USD_FIRST_PAGE) : undefined,
      ({ url }) =>
        url === '/accounts/acc-usd/transactions?page=1&size=20' ? json(USD_SECOND_PAGE) : undefined,
    );
    renderApp('/accounts?currency=USD', { loggedIn: true });

    expect(await screen.findByText('Оплата хостингу')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Доларовий рахунок/, pressed: true })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Сьогодні' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Вчора' })).toBeInTheDocument();
    expect(screen.queryByText('Підписка на курси')).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Показати ще' }));

    expect(await screen.findByText('Підписка на курси')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /^2\sлистопада$/ })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Показати ще' })).not.toBeInTheDocument();
  });

  it('tops up once even when the answer is lost and the client presses the button again', async () => {
    const keys: string[] = [];
    let attempts = 0;
    mockFetch(
      ({ url }) => layoutAnswer(url),
      ({ url }) => (url === '/accounts' ? json(ACCOUNTS) : undefined),
      ({ url }) => (url.startsWith('/accounts/acc-uah/transactions') ? json(EMPTY_PAGE) : undefined),
      ({ url, method, body, headers }) => {
        if (url !== '/payments/top-up' || method !== 'POST') {
          return undefined;
        }
        expect(body).toEqual({ accountId: 'acc-uah', amount: 1500.5 });
        keys.push(headers.get('Idempotency-Key') ?? '');
        attempts += 1;
        if (attempts === 1) {
          throw new TypeError('Failed to fetch');
        }
        return json({ id: 'p1', status: 'COMPLETED', currency: 'UAH', amount: 1500.5 }, 201);
      },
    );
    renderApp('/accounts', { loggedIn: true });

    await userEvent.type(await screen.findByLabelText('Сума'), '1 500,50');
    await userEvent.click(screen.getByRole('button', { name: 'Поповнити' }));
    expect(await screen.findByText(/Немає зв'язку з банком/)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Поповнити' }));

    expect(await screen.findByText(/^Зараховано 1\s500,50\s₴$/)).toBeInTheDocument();
    expect(keys).toHaveLength(2);
    expect(keys[0]).not.toBe('');
    expect(keys[1]).toBe(keys[0]);
  });

  it('explains a wrong amount before asking the bank', async () => {
    const fetchMock = mockFetch(
      ({ url }) => layoutAnswer(url),
      ({ url }) => (url === '/accounts' ? json(ACCOUNTS) : undefined),
      ({ url }) => (url.startsWith('/accounts/acc-uah/transactions') ? json(EMPTY_PAGE) : undefined),
    );
    renderApp('/accounts', { loggedIn: true });

    await userEvent.type(await screen.findByLabelText('Сума'), '200000');
    await userEvent.click(screen.getByRole('button', { name: 'Поповнити' }));

    expect(await screen.findByText(/^Від 0,01 до 100\s000 за одне поповнення$/)).toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([url]) => url === '/payments/top-up')).toBe(false);
  });

  it('opens an account in a currency the client does not have yet and switches to it', async () => {
    const hryvniaOnly = [ACCOUNTS[0]];
    const dollars = { ...ACCOUNTS[1], balance: 0 };
    let opened = false;
    mockFetch(
      ({ url }) => layoutAnswer(url),
      ({ url, method, body }) => {
        if (url === '/accounts' && method === 'POST') {
          expect(body).toEqual({ currency: 'USD' });
          opened = true;
          return json(dollars, 201);
        }
        return url === '/accounts' ? json(opened ? [...hryvniaOnly, dollars] : hryvniaOnly) : undefined;
      },
      ({ url }) => (url.includes('/transactions') ? json(EMPTY_PAGE) : undefined),
    );
    renderApp('/accounts', { loggedIn: true });

    const tiles = await screen.findByRole('list', { name: 'Ваші рахунки' });
    const dollarTile = within(tiles).getAllByRole('listitem')[1] as HTMLElement;
    expect(within(dollarTile).getByText('Ще не відкрито')).toBeInTheDocument();

    await userEvent.click(within(dollarTile).getByRole('button', { name: 'Відкрити' }));

    expect(
      await screen.findByRole('button', { name: /Доларовий рахунок/, pressed: true }),
    ).toBeInTheDocument();
  });
});
