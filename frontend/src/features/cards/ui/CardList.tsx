import { useTranslation } from 'react-i18next';
import type { Account } from '@/features/accounts/api/accountsApi';
import { cn } from '@/shared/lib/cn';
import { Panel, PanelMessage, PanelTitle } from '@/shared/ui/Panel/Panel';
import type { Card } from '../api/cardsApi';
import styles from './CardList.module.css';

interface CardListProps {
  cards: Card[];
  accounts: Account[];
  selectedId: string | undefined;
  onSelect: (cardId: string) => void;
}

/** All the client's cards, a working one first; a click shows it on the left. */
export function CardList({ cards, accounts, selectedId, onSelect }: CardListProps) {
  const { t } = useTranslation();
  const currencyOf = (card: Card) => accounts.find((account) => account.id === card.accountId)?.currency;

  return (
    <Panel className={styles.panel}>
      <PanelTitle className={styles.title}>{t('cards.list')}</PanelTitle>

      {cards.length === 0 ? (
        <div className={styles.message}>
          <PanelMessage>{t('cards.empty')}</PanelMessage>
        </div>
      ) : (
        <ul className={styles.list}>
          {cards.map((card) => {
            const chosen = card.id === selectedId;
            const active = card.status === 'ACTIVE';
            return (
              <li key={card.id}>
                <button
                  type="button"
                  className={cn(styles.row, chosen && styles.chosen)}
                  aria-pressed={chosen}
                  onClick={() => onSelect(card.id)}
                >
                  <span className={cn(styles.thumb, !active && styles.thumbInactive)} aria-hidden="true" />
                  <span className={styles.text}>
                    <span className={styles.name}>{t(`cards.types.${card.type}`)}</span>
                    <span className={styles.meta}>
                      •• {card.maskedNumber.slice(-4)} · {currencyOf(card)}
                      {/* In the second line, so a long card name keeps its full width. */}
                      {!active && (
                        <span className={styles.status}> · {t(`cards.statuses.${card.status}`)}</span>
                      )}
                    </span>
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </Panel>
  );
}
