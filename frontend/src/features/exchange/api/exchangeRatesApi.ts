import { request } from '@/shared/api/http';
import type { Currency } from '@/shared/lib/money';

export interface ExchangeRate {
  currency: Exclude<Currency, 'UAH'>;
  officialRate: number;
  officialRateDate: string;
  stale: boolean;
  /** What the bank pays for one unit. */
  buy: number;
  /** What the bank asks for one unit. */
  sell: number;
  updatedAt: string;
}

export const exchangeRatesApi = {
  list: () => request<ExchangeRate[]>('/exchange-rates', { authenticated: false }),
};
