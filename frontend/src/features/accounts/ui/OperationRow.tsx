import { useTranslation } from 'react-i18next';
import { currentLanguage } from '@/shared/i18n/i18n';
import { cn } from '@/shared/lib/cn';
import { formatMoney, formatSignedMoney, type Currency } from '@/shared/lib/money';
import type { StatementEntry } from '../api/accountsApi';
import styles from './OperationRow.module.css';

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

interface OperationRowProps {
  entry: StatementEntry;
  currency: Currency;
  /** "сьогодні, 13:42" on the home screen; only "13:42" in the statement, which shows the day above. */
  time: string;
}

/** One operation of the home screen and of the statement: who or what, when, how much, and the balance after. */
export function OperationRow({ entry, currency, time }: OperationRowProps) {
  const { t } = useTranslation();
  const language = currentLanguage();
  const incoming = entry.direction === 'CREDIT';
  const typeLabel = t(`operations.types.${entry.type}`);

  return (
    <li className={styles.row}>
      <span className={cn(styles.badge, incoming && styles.incoming)} aria-hidden="true">
        {badgeOf(entry)}
      </span>
      <span className={styles.text}>
        <span className={styles.name}>{entry.description ?? typeLabel}</span>
        <span className={styles.meta}>
          {typeLabel} · {time}
        </span>
      </span>
      <span className={styles.money}>
        <span className={cn(styles.amount, incoming && styles.incomingAmount)}>
          {formatSignedMoney(incoming ? entry.amount : -entry.amount, currency, language)}
        </span>
        <span className={styles.after}>
          {t('operations.balanceAfter', { amount: formatMoney(entry.balanceAfter, currency, language) })}
        </span>
      </span>
    </li>
  );
}
