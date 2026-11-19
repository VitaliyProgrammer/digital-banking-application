import { useTranslation } from 'react-i18next';
import { useSearchParams } from 'react-router';
import { ACCOUNT_CURRENCIES, clientAccountsByCurrency } from '@/features/accounts/model/clientAccounts';
import { useAccounts } from '@/features/accounts/model/queries';
import { Panel, PanelMessage } from '@/shared/ui/Panel/Panel';
import type { Card } from '../api/cardsApi';
import { primaryCard, useCards } from '../model/queries';
import { CardList } from '../ui/CardList';
import { CardPanel } from '../ui/CardPanel';
import { IssueCardForm } from '../ui/IssueCardForm';
import styles from './CardsPage.module.css';

const CARD_PARAM = 'card';

/** Working cards first, closed ones at the end: they stay in the list as history. */
function byStatus(cards: Card[]): Card[] {
  return [...cards].sort((a, b) => Number(a.status === 'CLOSED') - Number(b.status === 'CLOSED'));
}

/**
 * The client's cards: one in full on the left, the list and a new card on the right.
 * The chosen card is in the address (/cards?card=…), so the home screen can link straight to it.
 */
export function CardsPage() {
  const { t } = useTranslation();
  const cards = useCards();
  const accounts = useAccounts();
  const [searchParams, setSearchParams] = useSearchParams();

  if (cards.isError || accounts.isError) {
    return (
      <Panel className={styles.message}>
        <PanelMessage>{t('cards.loadFailed')}</PanelMessage>
      </Panel>
    );
  }

  if (!cards.data || !accounts.data) {
    return null;
  }

  const list = byStatus(cards.data);
  const card = list.find((item) => item.id === searchParams.get(CARD_PARAM)) ?? primaryCard(list) ?? list[0];
  const byCurrency = clientAccountsByCurrency(accounts.data);
  const everydayAccounts = ACCOUNT_CURRENCIES.flatMap((currency) => byCurrency[currency] ?? []);

  const select = (cardId: string) => setSearchParams({ [CARD_PARAM]: cardId }, { replace: true });

  return (
    <div className={styles.page}>
      <div className={styles.main}>
        {card ? (
          <CardPanel
            key={card.id}
            card={card}
            account={accounts.data.find((account) => account.id === card.accountId)}
          />
        ) : (
          <Panel className={styles.message}>
            <PanelMessage>{t('cards.empty')}</PanelMessage>
          </Panel>
        )}
      </div>

      <div className={styles.column}>
        {list.length > 0 && (
          <CardList cards={list} accounts={accounts.data} selectedId={card?.id} onSelect={select} />
        )}
        <IssueCardForm accounts={everydayAccounts} onIssued={(issued) => select(issued.id)} />
      </div>
    </div>
  );
}
