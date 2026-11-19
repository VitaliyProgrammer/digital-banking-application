import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { json, mockFetch, problem } from '@/test/fetchMock';
import { ACCOUNTS, CARDS } from '@/test/homeFixtures';
import { renderApp, TEST_USER } from '@/test/renderApp';

/** Valid like the bank's own: check digits by ISO 13616 and by Luhn. */
const RECIPIENT_IBAN = 'UA273009991234567890123456789';
const RECIPIENT_CARD = '4999991234567894';

/** A fake bank that prices transfers by its tariffs and remembers what it was asked to send. */
function mockTransfersBank({ refuseWith }: { refuseWith?: Response } = {}) {
  const sent: { url: string; body: unknown; key: string | null }[] = [];

  mockFetch(({ url, method, body, headers }) => {
    if (url === '/users/me') return json(TEST_USER);
    if (url === '/notifications/unread-count') return json({ unread: 0 });
    if (url === '/accounts') return json(ACCOUNTS);
    if (url === '/cards') return json(CARDS);

    const fee = /^\/payments\/(transfers|card-transfers)\/fee\?(.+)$/.exec(url);
    if (fee) {
      const amount = Number(new URLSearchParams(fee[2]).get('amount'));
      const charged = fee[1] === 'transfers' ? Math.max(amount * 0.005, 5) : amount * 0.005;
      return json({ currency: 'UAH', amount, fee: charged, total: amount + charged });
    }

    if (method === 'POST' && (url === '/payments/transfers' || url === '/payments/card-transfers')) {
      sent.push({ url, body, key: headers.get('Idempotency-Key') });
      if (refuseWith) return refuseWith;
      const { amount, description } = body as { amount: number; description?: string };
      return json(
        {
          id: 'pay-1',
          status: 'COMPLETED',
          currency: 'UAH',
          amount,
          fee: 5,
          description: description ?? 'Переказ на рахунок',
          createdAt: '2026-11-17T12:00:00Z',
        },
        201,
      );
    }
    return undefined;
  });

  return { sent };
}

/** People copy an IBAN or a card number from a message rather than type it; it is quicker in a test too. */
async function paste(field: HTMLElement, text: string) {
  await userEvent.click(field);
  await userEvent.paste(text);
}

describe('TransfersPage', () => {
  it('shows the fee before sending, sends by IBAN once and reads like a receipt afterwards', async () => {
    const bank = mockTransfersBank();
    renderApp('/transfers', { loggedIn: true });

    await paste(await screen.findByLabelText('IBAN отримувача'), RECIPIENT_IBAN.toLowerCase());
    await userEvent.type(screen.getByLabelText('Сума'), '1000');
    await paste(screen.getByLabelText('Призначення'), 'За оренду');

    expect(await screen.findByText(/^1\s005,00\s₴$/)).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: 'Переказати' }));

    expect(await screen.findByText('Гроші переказано')).toBeInTheDocument();
    expect(screen.getByText('UA27 3009 9912 3456')).toBeInTheDocument();
    expect(screen.getByText('7890 1234 5678 9')).toBeInTheDocument();
    expect(bank.sent).toHaveLength(1);
    expect(bank.sent[0]?.body).toEqual({
      fromAccountId: 'acc-uah',
      toAccountNumber: RECIPIENT_IBAN,
      amount: 1000,
      description: 'За оренду',
    });
    expect(bank.sent[0]?.key).toMatch(/^[0-9a-f-]{36}$/);
  });

  it('catches a mistyped IBAN before asking the bank', async () => {
    const bank = mockTransfersBank();
    renderApp('/transfers', { loggedIn: true });

    await paste(await screen.findByLabelText('IBAN отримувача'), 'UA273009991234567890123456798');
    await userEvent.type(screen.getByLabelText('Сума'), '100');
    await userEvent.click(screen.getByRole('button', { name: 'Переказати' }));

    expect(await screen.findByText('У IBAN помилка: перевірте цифри')).toBeInTheDocument();
    expect(bank.sent).toHaveLength(0);
  });

  it('sends from the chosen card to a card number', async () => {
    const bank = mockTransfersBank();
    renderApp('/transfers?to=card', { loggedIn: true });

    await paste(await screen.findByLabelText('Номер картки отримувача'), RECIPIENT_CARD);
    await userEvent.type(screen.getByLabelText('Сума'), '300');
    await userEvent.click(screen.getByRole('button', { name: 'Переказати' }));

    expect(await screen.findByText('•• 7894')).toBeInTheDocument();
    expect(bank.sent[0]).toMatchObject({
      url: '/payments/card-transfers',
      body: { fromCardId: 'card-1', toCardNumber: RECIPIENT_CARD, amount: 300 },
    });
  });

  it('shows the bank’s refusal under the field it is about', async () => {
    mockTransfersBank({ refuseWith: problem(404, 'recipient-not-found', 'Рахунок отримувача не знайдено') });
    renderApp('/transfers', { loggedIn: true });

    await paste(await screen.findByLabelText('IBAN отримувача'), RECIPIENT_IBAN);
    await userEvent.type(screen.getByLabelText('Сума'), '100');
    await userEvent.click(screen.getByRole('button', { name: 'Переказати' }));

    expect(await screen.findByText('Рахунок отримувача не знайдено')).toBeInTheDocument();
    expect(screen.getByLabelText('IBAN отримувача')).toHaveAttribute('aria-invalid', 'true');
  });
});
