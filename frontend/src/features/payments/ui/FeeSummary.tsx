import { useTranslation } from 'react-i18next';
import { currentLanguage } from '@/shared/i18n/i18n';
import { cn } from '@/shared/lib/cn';
import { formatMoney } from '@/shared/lib/money';
import type { FeeQuote } from '../api/paymentsApi';
import type { TransferSource } from './SourcePicker';
import styles from './FeeSummary.module.css';

interface FeeSummaryProps {
  source: TransferSource;
  /** Undefined until the amount is a valid one. */
  quote: FeeQuote | undefined;
  /** The quote shown belongs to the previous amount while the new one is priced. */
  stale: boolean;
  failed: boolean;
}

/**
 * The fee and the total before the client confirms. Both lines are always there, with a dash until there is
 * something to price, so the form never jumps while the client types.
 */
export function FeeSummary({ source, quote, stale, failed }: FeeSummaryProps) {
  const { t } = useTranslation();
  const language = currentLanguage();
  const money = (amount: number) => formatMoney(amount, source.currency, language);

  const fee = quote ? (quote.fee > 0 ? money(quote.fee) : t('transfers.fee.free')) : '—';
  const notEnough = quote !== undefined && !stale && quote.total > source.spendable;

  return (
    <div className={styles.summary} aria-live="polite">
      <dl className={cn(styles.rows, stale && styles.stale)}>
        <div className={styles.row}>
          <dt>{t('transfers.fee.fee')}</dt>
          <dd>{fee}</dd>
        </div>
        <div className={cn(styles.row, styles.total)}>
          <dt>{t('transfers.fee.total')}</dt>
          <dd>{quote ? money(quote.total) : '—'}</dd>
        </div>
      </dl>

      {failed && <p className={styles.note}>{t('transfers.fee.failed')}</p>}
      {notEnough && (
        <p className={cn(styles.note, styles.warning)}>
          {t('transfers.fee.notEnough', { amount: money(source.spendable) })}
        </p>
      )}
    </div>
  );
}
