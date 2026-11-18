import type { Language } from '@/shared/i18n/i18n';

/** The bank lives in Kyiv time: statements, interest and payouts are counted by it, so the screens use it too. */
export const BANK_TIME_ZONE = 'Europe/Kyiv';

const LOCALES: Record<Language, string> = { uk: 'uk-UA', en: 'en-GB' };

const EVENING_STARTS_AT_HOUR = 17;

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
