import { request } from '@/shared/api/http';

export type SpendingCategory = 'TRANSFERS' | 'SAVINGS' | 'UTILITIES' | 'MOBILE' | 'INTERNET';

export interface Spending {
  from: string;
  to: string;
  currency: 'UAH';
  total: number;
  /** Largest first; a category with nothing spent is left out. */
  categories: { category: SpendingCategory; amount: number }[];
}

export const spendingApi = {
  /** Both days included, "2026-11-01". */
  get: (from: string, to: string) => request<Spending>(`/payments/spending?from=${from}&to=${to}`),
};
