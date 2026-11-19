import { isoDate, kyivDayOf, type CalendarDay } from '@/shared/lib/kyivTime';
import type { StatementEntry } from '../api/accountsApi';

export interface StatementDay {
  /** "2026-11-17", in Kyiv time. */
  key: string;
  day: CalendarDay;
  entries: StatementEntry[];
}

/** Splits a statement, newest first, into Kyiv days, as a paper statement does. */
export function groupByKyivDay(entries: StatementEntry[]): StatementDay[] {
  const days: StatementDay[] = [];

  for (const entry of entries) {
    const day = kyivDayOf(new Date(entry.createdAt));
    const key = isoDate(day);
    const last = days.at(-1);

    if (last?.key === key) {
      last.entries.push(entry);
    } else {
      days.push({ key, day, entries: [entry] });
    }
  }

  return days;
}
