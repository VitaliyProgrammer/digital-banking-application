import { request } from '@/shared/api/http';
import type { Currency } from '@/shared/lib/money';

export interface SavingsJar {
  id: string;
  number: string;
  currency: Currency;
  balance: number;
  /** Percent a year: 12.00 */
  annualRate: number;
  /** Earned this month, paid out on the 1st of the next one. */
  accruedInterest: number;
  createdAt: string;
}

export const savingsApi = {
  list: () => request<SavingsJar[]>('/savings'),
};
