import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { currentLanguage } from '@/shared/i18n/i18n';
import { formatShortDateTime, formatTime, kyivDaysBetween } from './kyivTime';

/** "сьогодні, 13:42", "вчора, 19:20", "15 лист., 09:00": when an operation or a notification happened. */
export function useEventTime(now: Date) {
  const { t } = useTranslation();

  return useCallback(
    (iso: string): string => {
      const date = new Date(iso);
      const language = currentLanguage();
      const daysAgo = kyivDaysBetween(date, now);

      if (daysAgo === 0) {
        return t('time.today', { time: formatTime(date, language) });
      }
      if (daysAgo === 1) {
        return t('time.yesterday', { time: formatTime(date, language) });
      }
      return formatShortDateTime(date, language);
    },
    [t, now],
  );
}
