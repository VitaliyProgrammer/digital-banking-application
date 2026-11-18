import { isoDate, lastDayOfMonth, type CalendarDay } from '@/shared/lib/kyivTime';

export type SpendingPeriod = { kind: 'month'; year: number; month: number } | { kind: 'year'; year: number };

const MONTHS_TO_CHOOSE_FROM = 12;

/** The current month first, then the eleven before it. */
export function monthOptions(today: CalendarDay): SpendingPeriod[] {
  return Array.from({ length: MONTHS_TO_CHOOSE_FROM }, (_, back) => {
    const monthIndex = today.year * 12 + (today.month - 1) - back;
    return { kind: 'month', year: Math.floor(monthIndex / 12), month: (monthIndex % 12) + 1 };
  });
}

/** This year so far, and the whole previous year. */
export function yearOptions(today: CalendarDay): SpendingPeriod[] {
  return [
    { kind: 'year', year: today.year },
    { kind: 'year', year: today.year - 1 },
  ];
}

/** The days the bank is asked about, both included. */
export function periodRange(period: SpendingPeriod): { from: string; to: string } {
  if (period.kind === 'year') {
    return {
      from: isoDate({ year: period.year, month: 1, day: 1 }),
      to: isoDate({ year: period.year, month: 12, day: 31 }),
    };
  }
  return {
    from: isoDate({ year: period.year, month: period.month, day: 1 }),
    to: isoDate({ year: period.year, month: period.month, day: lastDayOfMonth(period.year, period.month) }),
  };
}

export function samePeriod(a: SpendingPeriod, b: SpendingPeriod): boolean {
  return (
    a.kind === b.kind &&
    a.year === b.year &&
    (a.kind === 'year' || (b.kind === 'month' && a.month === b.month))
  );
}
