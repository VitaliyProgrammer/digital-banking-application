import { request } from '@/shared/api/http';

export type ProviderCategory = 'ELECTRICITY' | 'WATER' | 'GAS' | 'HEATING' | 'INTERNET' | 'MOBILE';

export interface Cashback {
  /** "2026-11" */
  period: string;
  earned: number;
  monthlyCap: number;
  remaining: number;
  paidOut: boolean;
  /** Percent of a bill by its category: { MOBILE: 5.00, INTERNET: 3.00, … } */
  rates: Partial<Record<ProviderCategory, number>>;
}

export const cashbackApi = {
  currentMonth: () => request<Cashback>('/cashback'),
};
