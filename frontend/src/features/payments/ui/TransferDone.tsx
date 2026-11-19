import { useTranslation } from 'react-i18next';
import { currentLanguage } from '@/shared/i18n/i18n';
import { formatMoney } from '@/shared/lib/money';
import { Button } from '@/shared/ui/Button/Button';
import { ButtonLink } from '@/shared/ui/Button/ButtonLink';
import { CheckIcon } from '@/shared/ui/icons';
import type { TransferResult } from './TransferForm';
import styles from './TransferDone.module.css';

interface TransferDoneProps {
  result: TransferResult;
  onAnother: () => void;
}

/** What the bank did: the amount, from where, to whom and for what fee — the way a receipt reads. */
export function TransferDone({ result: { payment, source, recipient }, onAnother }: TransferDoneProps) {
  const { t } = useTranslation();
  const language = currentLanguage();
  const money = (amount: number) => formatMoney(amount, payment.currency, language);
  const completed = payment.status === 'COMPLETED';

  return (
    <div className={styles.done}>
      <div className={styles.head} role="status">
        <span className={styles.icon}>
          <CheckIcon size={26} />
        </span>
        <h3 className={styles.title}>
          {completed ? t('transfers.done.title') : t('transfers.done.processing')}
        </h3>
        <p className={styles.amount}>{money(payment.amount)}</p>
      </div>

      <dl className={styles.facts}>
        <div className={styles.fact}>
          <dt>{t('transfers.done.from')}</dt>
          <dd>
            {source.name} <span className={styles.mono}>{source.number}</span>
          </dd>
        </div>
        <div className={styles.fact}>
          <dt>{t('transfers.done.to')}</dt>
          <dd className={styles.mono}>
            {recipient.map((part) => (
              <span key={part} className={styles.part}>
                {part}
              </span>
            ))}
          </dd>
        </div>
        <div className={styles.fact}>
          <dt>{t('transfers.fee.fee')}</dt>
          <dd>{payment.fee > 0 ? money(payment.fee) : t('transfers.fee.free')}</dd>
        </div>
        {payment.description && (
          <div className={styles.fact}>
            <dt>{t('transfers.done.description')}</dt>
            <dd>{payment.description}</dd>
          </div>
        )}
      </dl>

      <div className={styles.buttons}>
        <Button onClick={onAnother}>{t('transfers.done.another')}</Button>
        <ButtonLink variant="soft" to={`/accounts?currency=${payment.currency}`}>
          {t('transfers.done.toAccount')}
        </ButtonLink>
      </div>
    </div>
  );
}
