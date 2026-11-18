import { useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import type { uk } from './locales/uk';

type MonthKey = keyof typeof uk.months.in;

function monthKey(month: number): MonthKey | null {
  return Number.isInteger(month) && month >= 1 && month <= 12 ? (`m${month}` as MonthKey) : null;
}

/** "в листопаді" / "in November": the month as the end of a sentence about when something happens. */
export function useMonthIn() {
  const { t } = useTranslation();

  return useCallback(
    (month: number): string => {
      const key = monthKey(month);
      return key ? t(`months.in.${key}`) : '';
    },
    [t],
  );
}
