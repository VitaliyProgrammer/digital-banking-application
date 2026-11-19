import type { Language } from '@/shared/i18n/i18n';

/** The bank lives in Kyiv time: statements, interest and payouts are counted by it, so the screens use it too. */
export const BANK_TIME_ZONE = 'Europe/Kyiv';

const LOCALES: Record<Language, string> = { uk: 'uk-UA', en: 'en-GB' };

const EVENING_STARTS_AT_HOUR = 17;

const NBSP = '\u00A0';

export function kyivHour(date: Date): number {
  return Number(
    new Intl.DateTimeFormat('en-GB', { hour: 'numeric', hourCycle: 'h23', timeZone: BANK_TIME_ZONE }).format(
      date,
    ),
  );
}

export function isEvening(date: Date): boolean {
  return kyivHour(date) >= EVENING_STARTS_AT_HOUR;
}

/** "Вівторок, 17 листопада" / "Tuesday, 17 November". */
export function formatLongDate(date: Date, language: Language): string {
  const text = new Intl.DateTimeFormat(LOCALES[language], {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    timeZone: BANK_TIME_ZONE,
  }).format(date);
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function formatTime(date: Date, language: Language): string {
  return new Intl.DateTimeFormat(LOCALES[language], {
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
    timeZone: BANK_TIME_ZONE,
  }).format(date);
}

/** "15 лист., 09:00" / "15 Nov, 09:00": for events older than yesterday. */
export function formatShortDateTime(date: Date, language: Language): string {
  return new Intl.DateTimeFormat(LOCALES[language], {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
    timeZone: BANK_TIME_ZONE,
  }).format(date);
}

/** A calendar day without time: the way the bank sends dates ("2026-12-31"). */
export interface CalendarDay {
  year: number;
  /** 1–12 */
  month: number;
  day: number;
}

/** The Kyiv day of a moment: an operation at 23:30 UTC on the 16th happened on the 17th in Kyiv. */
export function kyivDayOf(date: Date): CalendarDay {
  const [year, month, day] = new Intl.DateTimeFormat('en-CA', { timeZone: BANK_TIME_ZONE })
    .format(date)
    .split('-')
    .map(Number);
  return { year: year ?? 1970, month: month ?? 1, day: day ?? 1 };
}

/** Today in Kyiv, wherever the computer is. */
export function kyivToday(now: Date): CalendarDay {
  return kyivDayOf(now);
}

/** "2026-11-01" */
export function isoDate({ year, month, day }: CalendarDay): string {
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}

export function parseIsoDate(iso: string): CalendarDay {
  const [year, month, day] = iso.split('-').map(Number);
  return { year: year ?? 1970, month: month ?? 1, day: day ?? 1 };
}

export function lastDayOfMonth(year: number, month: number): number {
  return new Date(Date.UTC(year, month, 0)).getUTCDate();
}

export function nextMonth({ year, month }: Pick<CalendarDay, 'year' | 'month'>): CalendarDay {
  return month === 12 ? { year: year + 1, month: 1, day: 1 } : { year, month: month + 1, day: 1 };
}

/** "31 грудня" / "31 December", never split between two lines. */
export function formatDayMonth(date: CalendarDay, language: Language): string {
  return new Intl.DateTimeFormat(LOCALES[language], {
    day: 'numeric',
    month: 'long',
    timeZone: 'UTC',
  })
    .format(Date.UTC(date.year, date.month - 1, date.day))
    .replace(/\s/g, NBSP);
}

/** "листопад" / "November". */
export function monthName(month: number, language: Language): string {
  return new Intl.DateTimeFormat(LOCALES[language], { month: 'long', timeZone: 'UTC' }).format(
    Date.UTC(2000, month - 1, 1),
  );
}

/** How many Kyiv calendar days lie between two moments: 0 for the same day, 1 for yesterday. */
export function kyivDaysBetween(earlier: Date, later: Date): number {
  return Math.round((kyivDayNumber(later) - kyivDayNumber(earlier)) / 86_400_000);
}

function kyivDayNumber(date: Date): number {
  const [year, month, day] = new Intl.DateTimeFormat('en-CA', { timeZone: BANK_TIME_ZONE })
    .format(date)
    .split('-')
    .map(Number);
  return Date.UTC(year ?? 0, (month ?? 1) - 1, day ?? 1);
}
