import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import type { StatementEntry } from '@/features/accounts/api/accountsApi';
import { useStatement } from '@/features/accounts/model/queries';
import { currentLanguage } from '@/shared/i18n/i18n';
import { cn } from '@/shared/lib/cn';
import { formatMoney, formatSignedMoney } from '@/shared/lib/money';
import { useEventTime } from '@/shared/lib/useEventTime';
import { useNow } from '@/shared/lib/useNow';
import { Panel, PanelMessage, PanelTitle } from '@/shared/ui/Panel/Panel';
import { useMainAccount } from '../model/useMainAccount';
import styles from './RecentOperations.module.css';

const SHOWN = 6;

/** A short sign for an operation, as in the mockup: the first letter of the payee, or what kind of money it was. */
function badgeOf(entry: StatementEntry): string {
  switch (entry.type) {
    case 'DEPOSIT':
      return '+';
    case 'EXCHANGE':
      return '$';
    case 'INTEREST':
    case 'CASHBACK':
      return '%';
    case 'TRANSFER':
    case 'CARD_TRANSFER':
      return entry.direction === 'DEBIT' ? '→' : '←';
    default:
      return entry.description?.trim().charAt(0).toUpperCase() || '•';
  }
}

export function RecentOperations() {
  const { t } = useTranslation();
  const language = currentLanguage();
  const main = useMainAccount();
  const account = main.status === 'ready' ? main.account : undefined;
  const statement = useStatement(account?.id, SHOWN);
  const eventTime = useEventTime(useNow());

  return (
    <Panel className={styles.panel}>
      <div className={styles.header}>
        <PanelTitle>{t('home.operations.title')}</PanelTitle>
        <Link className={styles.all} to="/accounts">
          {t('home.operations.all')}
        </Link>
      </div>

      {(main.status === 'error' || statement.isError) && (
        <div className={styles.message}>
          <PanelMessage>{t('home.loadFailed')}</PanelMessage>
        </div>
      )}

      {statement.data?.length === 0 && (
        <div className={styles.message}>
          <PanelMessage>{t('home.operations.empty')}</PanelMessage>
        </div>
      )}

      {account && statement.data && statement.data.length > 0 && (
        <ul className={styles.list}>
          {statement.data.map((entry) => {
            const incoming = entry.direction === 'CREDIT';
            const amount = incoming ? entry.amount : -entry.amount;
            const typeLabel = t(`home.operations.types.${entry.type}`);
            return (
              <li key={`${entry.transactionId}-${entry.direction}`} className={styles.row}>
                <span className={cn(styles.badge, incoming && styles.incoming)} aria-hidden="true">
                  {badgeOf(entry)}
                </span>
                <span className={styles.text}>
                  <span className={styles.name}>{entry.description ?? typeLabel}</span>
                  <span className={styles.meta}>
                    {typeLabel} · {eventTime(entry.createdAt)}
                  </span>
                </span>
                <span className={styles.money}>
                  <span className={cn(styles.amount, incoming && styles.incomingAmount)}>
                    {formatSignedMoney(amount, account.currency, language)}
                  </span>
                  <span className={styles.after}>
                    {t('home.operations.balanceAfter', {
                      amount: formatMoney(entry.balanceAfter, account.currency, language),
                    })}
                  </span>
                </span>
              </li>
            );
          })}
        </ul>
      )}
    </Panel>
  );
}
