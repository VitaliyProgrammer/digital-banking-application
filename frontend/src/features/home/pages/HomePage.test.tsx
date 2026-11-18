import { screen, within } from '@testing-library/react';
import { json, mockFetch } from '@/test/fetchMock';
import { homeAnswer } from '@/test/homeFixtures';
import { renderApp, TEST_USER } from '@/test/renderApp';

function mockHomeBank() {
  return mockFetch(
    ({ url }) => (url === '/users/me' ? json(TEST_USER) : undefined),
    ({ url }) => {
      const answer = homeAnswer(url);
      return answer === undefined ? undefined : json(answer);
    },
  );
}

describe('HomePage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] });
    vi.setSystemTime(new Date('2026-11-17T12:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('shows the own money and how much is available with the credit limit', async () => {
    mockHomeBank();
    renderApp('/', { loggedIn: true });

    expect(await screen.findByText('Універсальна картка · гривня')).toBeInTheDocument();
    expect(screen.getByText(`68 215,30 ₴`)).toBeInTheDocument();
    expect(
      screen.getByText(`Витрати з ліміту в листопаді без відсотків до 31 грудня.`, { exact: false }),
    ).toBeInTheDocument();
  });

  it('embosses the transliterated name and never shows the full card number until the card is turned', async () => {
    const fetchMock = mockHomeBank();
    renderApp('/', { loggedIn: true });

    expect(await screen.findByText('IVAN PETRENKO')).toBeInTheDocument();
    expect(screen.getByText('•••• •••• •••• 2048')).toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([url]) => url.endsWith('/details'))).toBe(false);
  });

  it('splits the month by category, largest first, with shares that come from the bank', async () => {
    mockHomeBank();
    renderApp('/', { loggedIn: true });

    const legend = await screen.findByText('Перекази людям');
    const rows = within(legend.closest('ul') as HTMLElement).getAllByRole('listitem');

    expect(rows).toHaveLength(5);
    expect(rows[0]).toHaveTextContent(`Перекази людям44%4 500,00 ₴`);
    expect(
      // An accessible name keeps the non-breaking spaces of the amount, which \s matches.
      screen.getByRole('img', { name: /^Куди пішли гроші: Листопад 2026, усього 10\s230,00\s₴$/ }),
    ).toBeInTheDocument();
  });

  it('lists the latest operations with the balance after each one', async () => {
    mockHomeBank();
    renderApp('/', { loggedIn: true });

    expect(await screen.findByText('Мобільний Один, 380671234567')).toBeInTheDocument();
    expect(screen.getByText(`−250,00 ₴`)).toBeInTheDocument();
    expect(screen.getByText(`залишок 48 215,30 ₴`)).toBeInTheDocument();
  });

  it('shows the savings jar, the cashback with its limit and the bank rates', async () => {
    mockHomeBank();
    renderApp('/', { loggedIn: true });

    expect(await screen.findByText('12% річних')).toBeInTheDocument();
    expect(screen.getByText(`+96,79 ₴`)).toBeInTheDocument();
    expect(screen.getByText(`з 500 ₴ ліміту`)).toBeInTheDocument();
    expect(screen.getByText('Комуналка 1%')).toBeInTheDocument();
    expect(screen.getByText('41,35')).toBeInTheDocument();
  });
});
