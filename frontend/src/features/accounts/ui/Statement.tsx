import { useTranslation } from 'react-i18next';
import { currentLanguage } from '@/shared/i18n/i18n';
import { formatDayMonth, formatTime, kyivToday, type CalendarDay } from '@/shared/lib/kyivTime';
import { useNow } from '@/shared/lib/useNow';
import { Button } from '@/shared/ui/Button/Button';
import { Panel, PanelMessage, PanelTitle } from '@/shared/ui/Panel/Panel';
import type { Account } from '../api/accountsApi';
import { useStatement } from '../model/queries';
import { groupByKyivDay } from '../model/statementDays';
import { OperationRow } from './OperationRow';
import styles from './Statement.module.css';

function sameDay(a: CalendarDay, b: CalendarDay): boolean {
  return a.year === b.year && a.month === b.month && a.day === b.day;
}

function yesterdayOf(day: CalendarDay): CalendarDay {
  const date = new Date(Date.UTC(day.year, day.month - 1, day.day - 1));
  return { year: date.getUTCFullYear(), month: date.getUTCMonth() + 1, day: date.getUTCDate() };
}

/** The whole history of an account, by Kyiv days, newest first. */
export function Statement({ account }: { account: Account }) {
  const { t } = useTranslation();
  const language = currentLanguage();
  const today = kyivToday(useNow());
  const statement = useStatement(account.id);

  const dayTitle = (day: CalendarDay): string => {
    if (sameDay(day, today)) {
      return t('accounts.statement.today');
    }
    if (sameDay(day, yesterdayOf(today))) {
      return t('accounts.statement.yesterday');
    }
    const dayMonth = formatDayMonth(day, language);
    return day.year === today.year ? dayMonth : `${dayMonth} ${day.year}`;
  };

  return (
    <Panel className={styles.panel}>
      <div className={styles.header}>
        <PanelTitle>{t('accounts.statement.title')}</PanelTitle>
      </div>

      {statement.isError && (
        <div className={styles.message}>
          <PanelMessage>{t('accounts.statement.loadFailed')}</PanelMessage>
        </div>
      )}

      {statement.data?.length === 0 && (
        <div className={styles.message}>
          <PanelMessage>{t('accounts.statement.empty')}</PanelMessage>
        </div>
      )}

      {statement.data && statement.data.length > 0 && (
        <div className={styles.days}>
          {groupByKyivDay(statement.data).map(({ key, day, entries }) => (
            <section key={key} aria-labelledby={`day-${key}`}>
              <h3 id={`day-${key}`} className={styles.day}>
                {dayTitle(day)}
              </h3>
              <ul className={styles.list}>
                {entries.map((entry) => (
                  <OperationRow
                    key={`${entry.transactionId}-${entry.direction}`}
                    entry={entry}
                    currency={account.currency}
                    time={formatTime(new Date(entry.createdAt), language)}
                  />
                ))}
              </ul>
            </section>
          ))}
        </div>
      )}

      {statement.hasNextPage && (
        <div className={styles.more}>
          <Button
            variant="soft"
            loading={statement.isFetchingNextPage}
            onClick={() => void statement.fetchNextPage()}
          >
            {t('accounts.statement.more')}
          </Button>
        </div>
      )}
    </Panel>
  );
}
