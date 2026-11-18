import type { Account } from '@/features/accounts/api/accountsApi';
import { useAccounts } from '@/features/accounts/model/queries';
import type { Card } from '@/features/cards/api/cardsApi';
import { primaryCard, useCards } from '@/features/cards/model/queries';
import type { CreditLine } from '@/features/credit/api/creditLineApi';
import { useCreditLine } from '@/features/credit/model/queries';

export type MainAccount =
  | { status: 'loading' }
  | { status: 'error' }
  | { status: 'ready'; account: Account | undefined; card: Card | undefined; creditLine: CreditLine | null };

/**
 * What the home screen is about: the main card, its account and, if the bank opened one on it, the credit line.
 * Without a card it is the client's hryvnia account.
 */
export function useMainAccount(): MainAccount {
  const accounts = useAccounts();
  const cards = useCards();
  const creditLine = useCreditLine();

  if (accounts.isError || cards.isError || creditLine.isError) {
    return { status: 'error' };
  }
  if (!accounts.data || !cards.data || creditLine.isPending) {
    return { status: 'loading' };
  }

  const card = primaryCard(cards.data);
  const account = card
    ? accounts.data.find((candidate) => candidate.id === card.accountId)
    : accounts.data.find((candidate) => candidate.type === 'CLIENT' && candidate.currency === 'UAH');
  const line =
    creditLine.data && account && creditLine.data.accountId === account.id ? creditLine.data : null;

  return { status: 'ready', account, card, creditLine: line };
}
