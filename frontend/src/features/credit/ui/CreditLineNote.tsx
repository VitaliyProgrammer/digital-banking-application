import { useTranslation } from 'react-i18next';
import { currentLanguage } from '@/shared/i18n/i18n';
import { useMonthIn } from '@/shared/i18n/useMonthIn';
import { formatDayMonth, kyivToday, parseIsoDate } from '@/shared/lib/kyivTime';
import { formatMoney } from '@/shared/lib/money';
import { useNow } from '@/shared/lib/useNow';
import type { CreditLine } from '../api/creditLineApi';
import styles from './CreditLineNote.module.css';

/** The grace period under the card: when the client must repay, if at all, to pay no interest. */
export function CreditLineNote({ creditLine }: { creditLine: CreditLine }) {
  const { t } = useTranslation();
  const language = currentLanguage();
  const monthIn = useMonthIn();
  const today = kyivToday(useNow());

  const owesForStatement = creditLine.leftToRepayInterestFree > 0 && creditLine.statementInterestFreeUntil;

  const title =
    creditLine.debt > 0
      ? t('home.credit.debt', { amount: formatMoney(creditLine.debt, 'UAH', language) })
      : t('home.credit.noDebt');

  const text = owesForStatement
    ? t('home.credit.repayInterestFree', {
        amount: formatMoney(creditLine.leftToRepayInterestFree, 'UAH', language),
        date: formatDayMonth(parseIsoDate(creditLine.statementInterestFreeUntil ?? ''), language),
      })
    : t('home.credit.newSpending', {
        month: monthIn(today.month),
        date: formatDayMonth(parseIsoDate(creditLine.newSpendingInterestFreeUntil), language),
      });

  return (
    <p className={styles.note}>
      <ClockIcon />
      <span>
        <strong>{title}</strong> {text}
      </span>
    </p>
  );
}

function ClockIcon() {
  return (
    <svg
      className={styles.icon}
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2.2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </svg>
  );
}
