import { request } from '@/shared/api/http';
import type { Page } from '@/shared/api/page';
import type { Currency } from '@/shared/lib/money';

export type AccountType = 'CLIENT' | 'SAVINGS' | 'CREDIT' | 'BUSINESS' | 'TAX_RESERVE';

export interface Account {
  id: string;
  number: string;
  currency: Currency;
  type: AccountType;
  status: 'ACTIVE' | 'FROZEN' | 'CLOSED';
  balance: number;
  creditLimit: number | null;
  createdAt: string;
}

export type TransactionType =
  | 'DEPOSIT'
  | 'TRANSFER'
  | 'CARD_TRANSFER'
  | 'HOLD'
  | 'CAPTURE'
  | 'RELEASE'
  | 'EXCHANGE'
  | 'INTEREST'
  | 'CASHBACK'
  | 'CREDIT_CHARGES';

export interface StatementEntry {
  transactionId: string;
  type: TransactionType;
  direction: 'DEBIT' | 'CREDIT';
  amount: number;
  balanceAfter: number;
  description: string | null;
  createdAt: string;
}

export const accountsApi = {
  list: () => request<Account[]>('/accounts'),

  statement: (accountId: string, size: number) =>
    request<Page<StatementEntry>>(`/accounts/${accountId}/transactions?page=0&size=${size}`),
};
