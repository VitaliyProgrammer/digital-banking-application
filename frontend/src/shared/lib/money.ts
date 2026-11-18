import type { Language } from '@/shared/i18n/i18n';

export type Currency = 'UAH' | 'USD' | 'EUR';

const LOCALES: Record<Language, string> = { uk: 'uk-UA', en: 'en-GB' };

/** As in the approved mockup: the hryvnia sign after the number, the dollar and euro signs before it. */
const SIGNS: Record<Currency, { sign: string; before: boolean }> = {
  UAH: { sign: '₴', before: false },
  USD: { sign: '$', before: true },
  EUR: { sign: '€', before: true },
};

const NBSP = '\u00A0';

/** The bank sends money as decimal strings or numbers ("48215.3"); they are only shown, never counted with. */
export function formatAmount(amount: number | string, language: Language): string {
  return new Intl.NumberFormat(LOCALES[language], {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })
    .format(Math.abs(Number(amount)))
    .replace(/\s/g, NBSP);
}

/** A rate or a limit: "12", "1,5" — no zeros after the comma when there is nothing there. */
export function formatNumber(value: number | string, language: Language): string {
  return new Intl.NumberFormat(LOCALES[language], { maximumFractionDigits: 2 })
    .format(Number(value))
    .replace(/\s/g, NBSP);
}

interface MoneyOptions {
  /** "500 ₴" instead of "500,00 ₴": for limits and caps, not for balances. */
  wholeIfRound?: boolean;
}

/** "48 215,30 ₴", "$ 1 240,00". The parts never break apart at the end of a line. */
export function formatMoney(
  amount: number | string,
  currency: Currency,
  language: Language,
  { wholeIfRound = false }: MoneyOptions = {},
): string {
  const { sign, before } = SIGNS[currency];
  const round = wholeIfRound && Number.isInteger(Number(amount));
  const number = round ? formatNumber(Math.abs(Number(amount)), language) : formatAmount(amount, language);
  const minus = Number(amount) < 0 ? '−' : '';
  return before ? `${minus}${sign}${NBSP}${number}` : `${minus}${number}${NBSP}${sign}`;
}

/** "+2 000,00 ₴" / "−250,00 ₴" for operations. */
export function formatSignedMoney(amount: number | string, currency: Currency, language: Language): string {
  const sign = Number(amount) < 0 ? '−' : '+';
  return sign + formatMoney(Math.abs(Number(amount)), currency, language);
}
