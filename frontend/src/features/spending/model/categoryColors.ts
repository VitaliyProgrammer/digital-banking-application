import type { SpendingCategory } from '../api/spendingApi';

/** One colour per category, the same in the chart and its legend. */
export const CATEGORY_COLORS: Record<SpendingCategory, string> = {
  TRANSFERS: 'var(--spending-transfers)',
  SAVINGS: 'var(--spending-savings)',
  UTILITIES: 'var(--spending-utilities)',
  MOBILE: 'var(--spending-mobile)',
  INTERNET: 'var(--spending-internet)',
};
