import { ApiError } from '@/shared/api/ApiError';
import { request } from '@/shared/api/http';

export interface CreditLine {
  accountId: string;
  number: string;
  creditLimit: number;
  /** The client's own money on the account, spent before the limit. */
  ownFunds: number;
  /** What may still be spent: own money plus the unused limit. */
  available: number;
  debt: number;
  statementDebt: number;
  leftToRepayInterestFree: number;
  minimumPayment: number;
  leftForMinimumPayment: number;
  /** "2026-11-30" */
  statementInterestFreeUntil: string | null;
  newSpendingInterestFreeUntil: string;
}

export const creditLineApi = {
  /** The client's credit line, or null when the bank has not opened one. */
  find: async (): Promise<CreditLine | null> => {
    try {
      return await request<CreditLine>('/credit-line');
    } catch (error) {
      if (error instanceof ApiError && error.code === 'credit-line-not-found') {
        return null;
      }
      throw error;
    }
  },
};
