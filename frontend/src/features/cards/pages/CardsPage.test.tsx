import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { json, mockFetch, problem } from '@/test/fetchMock';
import { ACCOUNTS, CARDS } from '@/test/homeFixtures';
import { renderApp, TEST_USER } from '@/test/renderApp';

const UNIVERSAL = CARDS[0] as (typeof CARDS)[number];

const VIRTUAL = {
  ...UNIVERSAL,
  id: 'card-2',
  type: 'VIRTUAL',
  maskedNumber: '**** **** **** 7310',
  pinSet: false,
  createdAt: '2026-11-10T09:00:00Z',
};

/** A fake bank that keeps the cards it is told about, so a change shows on the page like it would for real. */
function mockCardsBank(initialCards: unknown[]) {
  let cards = [...initialCards] as (typeof UNIVERSAL)[];
  const requests: { url: string; method: string; body: unknown }[] = [];

  const fetchMock = mockFetch(({ url, method, body }) => {
    requests.push({ url, method, body });
    if (url === '/users/me') return json(TEST_USER);
    if (url === '/notifications/unread-count') return json({ unread: 0 });
    if (url === '/accounts') return json(ACCOUNTS);
    if (url === '/cards' && method === 'GET') return json(cards);
    if (url === '/cards' && method === 'POST') {
      const issued = { ...VIRTUAL, id: 'card-new', maskedNumber: '**** **** **** 5521' };
      cards = [...cards, issued];
      return json(issued, 201);
    }
    const action = /^\/cards\/([\w-]+)\/(block|unblock|close)$/.exec(url);
    if (action) {
      const status = { block: 'BLOCKED', unblock: 'ACTIVE', close: 'CLOSED' }[action[2] as 'block'];
      cards = cards.map((card) => (card.id === action[1] ? { ...card, status } : card));
      return json(cards.find((card) => card.id === action[1]));
    }
    return undefined;
  });

  return {
    fetchMock,
    requests,
    answerPin: (answer: (body: unknown) => Response) =>
      fetchMock.mockImplementation(async (url: string, init: RequestInit = {}) => {
        if (url.endsWith('/pin') && init.method === 'PUT') {
          return answer(JSON.parse(init.body as string));
        }
        if (url === '/users/me') return json(TEST_USER);
        if (url === '/notifications/unread-count') return json({ unread: 0 });
        if (url === '/accounts') return json(ACCOUNTS);
        return json(cards);
      }),
  };
}

describe('CardsPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date('2026-11-17T12:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows the card from the address with its facts, and the list of all cards', async () => {
    mockCardsBank([UNIVERSAL, VIRTUAL]);
    renderApp('/cards?card=card-2', { loggedIn: true });

    const list = await screen.findByRole('heading', { name: 'Мої картки' });
    const rows = within(list.closest('section') as HTMLElement).getAllByRole('button');

    expect(rows).toHaveLength(2);
    expect(screen.getByRole('button', { name: /Віртуальна картка/, pressed: true })).toBeInTheDocument();
    expect(screen.getByText('ще не встановлено')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Встановити PIN' })).toBeInTheDocument();
  });

  it('blocks a card and offers to unblock it', async () => {
    const bank = mockCardsBank([UNIVERSAL]);
    renderApp('/cards', { loggedIn: true });

    await userEvent.click(await screen.findByRole('button', { name: 'Заблокувати' }));

    expect(await screen.findByRole('button', { name: 'Розблокувати' })).toBeInTheDocument();
    expect(screen.getByText(/Ви заблокували картку/)).toBeInTheDocument();
    expect(bank.requests.some(({ url, method }) => url === '/cards/card-1/block' && method === 'POST')).toBe(
      true,
    );
  });

  it('closes a card only after the client confirms', async () => {
    const bank = mockCardsBank([UNIVERSAL]);
    renderApp('/cards', { loggedIn: true });

    await userEvent.click(await screen.findByRole('button', { name: 'Закрити картку' }));
    expect(screen.getByText(/Закриту картку не можна відновити/)).toBeInTheDocument();
    expect(bank.requests.some(({ url }) => url.endsWith('/close'))).toBe(false);

    await userEvent.click(screen.getByRole('button', { name: 'Так, закрити' }));

    expect(await screen.findByText('Картку закрито назавжди.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Заблокувати' })).not.toBeInTheDocument();
  });

  it('changes the PIN with the current one and shows the bank’s refusal of a wrong one', async () => {
    const bank = mockCardsBank([UNIVERSAL]);
    renderApp('/cards', { loggedIn: true });
    await userEvent.click(await screen.findByRole('button', { name: 'Змінити PIN' }));

    bank.answerPin(() => problem(422, 'wrong-pin', 'Неправильний PIN. Залишилось спроб: 2'));
    await userEvent.type(screen.getByLabelText('Поточний PIN'), '1357');
    await userEvent.type(screen.getByLabelText('Новий PIN'), '4825');
    await userEvent.type(screen.getByLabelText('Повторіть новий PIN'), '4825');
    await userEvent.click(screen.getByRole('button', { name: 'Зберегти PIN' }));

    expect(await screen.findByText('Неправильний PIN. Залишилось спроб: 2')).toBeInTheDocument();

    const sent: unknown[] = [];
    bank.answerPin((body) => {
      sent.push(body);
      return new Response(null, { status: 204 });
    });
    await userEvent.click(screen.getByRole('button', { name: 'Зберегти PIN' }));

    expect(await screen.findByText('PIN збережено')).toBeInTheDocument();
    expect(sent).toEqual([{ currentPin: '1357', newPin: '4825' }]);
  });

  it('refuses a PIN repeated differently before asking the bank', async () => {
    const bank = mockCardsBank([VIRTUAL]);
    renderApp('/cards', { loggedIn: true });
    await userEvent.click(await screen.findByRole('button', { name: 'Встановити PIN' }));

    await userEvent.type(screen.getByLabelText('Новий PIN'), '4825');
    await userEvent.type(screen.getByLabelText('Повторіть новий PIN'), '4826');
    await userEvent.click(screen.getByRole('button', { name: 'Зберегти PIN' }));

    expect(await screen.findByText('PIN-коди не збігаються')).toBeInTheDocument();
    expect(bank.requests.some(({ url }) => url.endsWith('/pin'))).toBe(false);
  });

  it('issues a new card on the chosen account and opens it', async () => {
    const bank = mockCardsBank([UNIVERSAL]);
    renderApp('/cards', { loggedIn: true });

    await userEvent.click(await screen.findByRole('radio', { name: /Студентська картка/ }));
    await userEvent.click(screen.getByRole('radio', { name: 'USD' }));
    await userEvent.click(screen.getByRole('button', { name: 'Випустити картку' }));

    expect(await screen.findByRole('button', { name: /•• 5521/, pressed: true })).toBeInTheDocument();
    expect(bank.requests.find(({ url, method }) => url === '/cards' && method === 'POST')?.body).toEqual({
      accountId: 'acc-usd',
      type: 'STUDENT',
    });
  });
});
