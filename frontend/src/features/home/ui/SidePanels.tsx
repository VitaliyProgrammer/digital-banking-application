import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { useAccounts } from '@/features/accounts/model/queries';
import { useCurrentCashback } from '@/features/cashback/model/queries';
import { useExchangeRates } from '@/features/exchange/model/queries';
import { useSavingsJars } from '@/features/savings/model/queries';
import { currentLanguage } from '@/shared/i18n/i18n';
import { formatDayMonth, kyivToday, monthName, nextMonth } from '@/shared/lib/kyivTime';
import { formatAmount, formatMoney, formatNumber, formatSignedMoney } from '@/shared/lib/money';
import { useNow } from '@/shared/lib/useNow';
import { ButtonLink } from '@/shared/ui/Button/ButtonLink';
import { Panel, PanelMessage, PanelTitle } from '@/shared/ui/Panel/Panel';
import { Pill } from '@/shared/ui/Pill/Pill';
import styles from './SidePanels.module.css';

const FOREIGN_CURRENCIES = ['USD', 'EUR'] as const;

/** "1 грудня": when this month's interest and cashback are paid. */
function usePayoutDate(): { month: string; payout: string } {
  const language = currentLanguage();
  const today = kyivToday(useNow());
  return {
    month: monthName(today.month, language),
    payout: formatDayMonth(nextMonth(today), language),
  };
}

export function CurrencyAccountsPanel() {
  const { t } = useTranslation();
  const language = currentLanguage();
  const accounts = useAccounts();

  return (
    <Panel className={styles.panel}>
      <PanelTitle className={styles.titleSpaced}>{t('home.currency.title')}</PanelTitle>
      {accounts.isError ? (
        <PanelMessage>{t('home.loadFailed')}</PanelMessage>
      ) : (
        <div className={styles.currencies}>
          {FOREIGN_CURRENCIES.map((currency) => {
            const account = accounts.data?.find(
              (item) => item.type === 'CLIENT' && item.currency === currency,
            );
            return (
              <Link key={currency} className={styles.currency} to={`/accounts?currency=${currency}`}>
                <span className={styles.currencyName}>{t(`home.currency.names.${currency}`)}</span>
                <span className={account ? styles.currencyAmount : styles.currencyOpen}>
                  {account ? formatMoney(account.balance, currency, language) : t('home.currency.open')}
                </span>
              </Link>
            );
          })}
        </div>
      )}
    </Panel>
  );
}

export function SavingsPanel() {
  const { t } = useTranslation();
  const language = currentLanguage();
  const jars = useSavingsJars();
  const { month, payout } = usePayoutDate();
  const jar = jars.data?.find((item) => item.currency === 'UAH');

  if (jars.isError) {
    return (
      <Panel className={styles.panel}>
        <PanelMessage>{t('home.loadFailed')}</PanelMessage>
      </Panel>
    );
  }

  return (
    <Panel className={styles.panel}>
      <div className={styles.titleRow}>
        <PanelTitle>{t('home.savings.title')}</PanelTitle>
        {jar && (
          <Pill
            size="large"
            colors={{ background: 'var(--pill-savings)', color: 'var(--pill-savings-text)' }}
          >
            {t('home.savings.rate', { rate: formatNumber(jar.annualRate, language) })}
          </Pill>
        )}
      </div>

      {jar ? (
        <>
          <p className={styles.bigAmount}>{formatMoney(jar.balance, 'UAH', language)}</p>
          <div className={styles.accruedRow}>
            <span>{t('home.savings.accrued', { month })}</span>
            <strong className={styles.accrued}>
              {formatSignedMoney(jar.accruedInterest, 'UAH', language)}
            </strong>
          </div>
          <p className={styles.note}>{t('home.savings.note', { date: payout })}</p>
        </>
      ) : (
        jars.data && (
          <div className={styles.empty}>
            <PanelMessage>{t('home.savings.empty')}</PanelMessage>
            <ButtonLink variant="soft" to="/savings">
              {t('home.savings.open')}
            </ButtonLink>
          </div>
        )
      )}
    </Panel>
  );
}

const CASHBACK_PILLS = [
  {
    key: 'MOBILE',
    categories: ['MOBILE'],
    colors: { background: 'var(--pill-mobile)', color: 'var(--pill-mobile-text)' },
  },
  {
    key: 'INTERNET',
    categories: ['INTERNET'],
    colors: { background: 'var(--pill-internet)', color: 'var(--pill-internet-text)' },
  },
  {
    key: 'UTILITIES',
    categories: ['ELECTRICITY', 'WATER', 'GAS', 'HEATING'],
    colors: { background: 'var(--pill-utilities)', color: 'var(--pill-utilities-text)' },
  },
] as const;

export function CashbackPanel() {
  const { t } = useTranslation();
  const language = currentLanguage();
  const cashback = useCurrentCashback();
  const { month, payout } = usePayoutDate();

  if (cashback.isError) {
    return (
      <Panel className={styles.panel}>
        <PanelMessage>{t('home.loadFailed')}</PanelMessage>
      </Panel>
    );
  }

  const data = cashback.data;
  const percent =
    data && data.monthlyCap > 0 ? Math.min(100, Math.round((data.earned / data.monthlyCap) * 100)) : 0;

  return (
    <Panel className={styles.panel}>
      <div className={styles.titleRow}>
        <PanelTitle>{t('home.cashback.title', { month })}</PanelTitle>
        <span className={styles.payout}>
          {data?.paidOut ? t('home.cashback.paid') : t('home.cashback.payout', { date: payout })}
        </span>
      </div>

      {data && (
        <>
          <p className={styles.bigAmount}>
            {formatMoney(data.earned, 'UAH', language)}{' '}
            <span className={styles.ofCap}>
              {t('home.cashback.ofCap', {
                cap: formatMoney(data.monthlyCap, 'UAH', language, { wholeIfRound: true }),
              })}
            </span>
          </p>
          <div className={styles.progress} role="img" aria-label={t('home.cashback.progress', { percent })}>
            <div className={styles.progressFill} style={{ width: `${percent}%` }} />
          </div>
          <div className={styles.pills}>
            {CASHBACK_PILLS.map(({ key, categories, colors }) => {
              const rate = categories
                .map((category) => data.rates[category])
                .find((value) => value !== undefined);
              return (
                rate !== undefined && (
                  <Pill key={key} size="large" colors={colors}>
                    {t(`home.cashback.rates.${key}`, { rate: formatNumber(rate, language) })}
                  </Pill>
                )
              );
            })}
          </div>
        </>
      )}
    </Panel>
  );
}

export function BankRatesPanel() {
  const { t } = useTranslation();
  const language = currentLanguage();
  const rates = useExchangeRates();

  return (
    <Panel className={styles.panel}>
      <PanelTitle className={styles.titleSpaced}>{t('home.rates.title')}</PanelTitle>
      {rates.isError ? (
        <PanelMessage>{t('home.loadFailed')}</PanelMessage>
      ) : (
        <div className={styles.rates}>
          <span className={styles.ratesHead}>{t('home.rates.currency')}</span>
          <span className={styles.ratesHead}>{t('home.rates.buy')}</span>
          <span className={styles.ratesHead}>{t('home.rates.sell')}</span>
          {rates.data?.map((rate) => (
            <div key={rate.currency} className={styles.ratesRow}>
              <strong>{rate.currency}</strong>
              <span>{formatAmount(rate.buy, language)}</span>
              <span>{formatAmount(rate.sell, language)}</span>
            </div>
          ))}
        </div>
      )}
    </Panel>
  );
}
