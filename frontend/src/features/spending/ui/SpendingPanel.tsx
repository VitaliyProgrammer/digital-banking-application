import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { currentLanguage } from '@/shared/i18n/i18n';
import { kyivToday, monthName } from '@/shared/lib/kyivTime';
import { formatMoney } from '@/shared/lib/money';
import { useNow } from '@/shared/lib/useNow';
import { Panel, PanelMessage, PanelTitle } from '@/shared/ui/Panel/Panel';
import { monthOptions, periodRange, samePeriod, yearOptions, type SpendingPeriod } from '../model/periods';
import { CATEGORY_COLORS } from '../model/categoryColors';
import { useSpending } from '../model/queries';
import { PeriodPicker, type PeriodOption } from './PeriodPicker';
import { SpendingDonut } from './SpendingDonut';
import styles from './SpendingPanel.module.css';

function capitalize(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1);
}

/** "Куди пішли гроші": the client's spending of a month or a year by category, from the bank's own sums. */
export function SpendingPanel() {
  const { t } = useTranslation();
  const language = currentLanguage();
  const today = kyivToday(useNow());

  const currentMonth: SpendingPeriod = { kind: 'month', year: today.year, month: today.month };
  const [period, setPeriod] = useState<SpendingPeriod>(currentMonth);

  const options: PeriodOption[] =
    period.kind === 'month'
      ? monthOptions(today).map((option, index) => ({
          period: option,
          label:
            option.kind === 'month' ? `${capitalize(monthName(option.month, language))} ${option.year}` : '',
          hint: index === 0 ? t('home.spending.current') : undefined,
        }))
      : yearOptions(today).map((option, index) => ({
          period: option,
          label: t('home.spending.yearLabel', { year: option.year }),
          hint:
            index === 0
              ? `${monthName(1, language)} — ${monthName(today.month, language)}`
              : t('home.spending.fullYear'),
        }));

  const { from, to } = periodRange(period);
  const spending = useSpending(from, to);
  const selectedLabel = options.find((option) => samePeriod(option.period, period))?.label ?? '';

  const changeKind = (kind: SpendingPeriod['kind']) => {
    if (kind !== period.kind) {
      setPeriod(kind === 'month' ? currentMonth : { kind: 'year', year: today.year });
    }
  };

  const total = spending.data?.total ?? 0;
  const slices = spending.data?.categories ?? [];
  const totalText = formatMoney(total, 'UAH', language);

  return (
    <Panel className={styles.panel}>
      <div className={styles.header}>
        <div>
          <PanelTitle>{t('home.spending.title')}</PanelTitle>
          <p className={styles.subtitle}>{t('home.spending.subtitle')}</p>
        </div>
        <PeriodPicker
          kind={period.kind}
          onKindChange={changeKind}
          options={options}
          selected={period}
          onSelect={setPeriod}
        />
      </div>

      {spending.isError ? (
        <PanelMessage>{t('home.loadFailed')}</PanelMessage>
      ) : (
        <div className={styles.body}>
          <SpendingDonut
            slices={slices}
            total={total}
            totalLabel={t('home.spending.total')}
            totalText={totalText}
            description={t('home.spending.chart', { period: selectedLabel, total: totalText })}
          />

          {spending.data && slices.length === 0 && <PanelMessage>{t('home.spending.empty')}</PanelMessage>}

          <ul className={styles.legend}>
            {slices.map(({ category, amount }) => (
              <li key={category} className={styles.legendRow}>
                <span className={styles.swatch} style={{ background: CATEGORY_COLORS[category] }} />
                <span className={styles.category}>{t(`home.spending.categories.${category}`)}</span>
                <span className={styles.percent}>{Math.round((amount / total) * 100)}%</span>
                <span className={styles.amount}>{formatMoney(amount, 'UAH', language)}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </Panel>
  );
}
