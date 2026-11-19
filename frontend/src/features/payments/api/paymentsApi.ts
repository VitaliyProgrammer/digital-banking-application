import { request } from '@/shared/api/http';
import type { Currency } from '@/shared/lib/money';

export type PaymentStatus = 'PENDING' | 'PROCESSING' | 'UNKNOWN' | 'COMPLETED' | 'FAILED';

/** The part of the bank's payment the screens use so far; the rest arrives with the payments section. */
export interface Payment {
  id: string;
  status: PaymentStatus;
  currency: Currency;
  amount: number;
  fee: number;
  description: string | null;
  createdAt: string;
}

export interface TopUpRequest {
  accountId: string;
  amount: number;
}

export interface TransferRequest {
  fromAccountId: string;
  /** The recipient's IBAN; the bank accepts it with spaces, the way people copy it. */
  toAccountNumber: string;
  amount: number;
  description?: string;
}

export interface CardTransferRequest {
  fromCardId: string;
  toCardNumber: string;
  amount: number;
  description?: string;
}

/** What a transfer will cost, priced by the same tariff the transfer itself is charged by. */
export interface FeeQuote {
  currency: Currency;
  amount: number;
  fee: number;
  /** Amount and fee: what leaves the sender's account. */
  total: number;
}

/** The key makes a repeated request return the first payment instead of paying twice. */
const withKey = (idempotencyKey: string) => ({ 'Idempotency-Key': idempotencyKey });

export const paymentsApi = {
  topUp: (topUp: TopUpRequest, idempotencyKey: string) =>
    request<Payment>('/payments/top-up', { method: 'POST', body: topUp, headers: withKey(idempotencyKey) }),

  transfer: (transfer: TransferRequest, idempotencyKey: string) =>
    request<Payment>('/payments/transfers', {
      method: 'POST',
      body: transfer,
      headers: withKey(idempotencyKey),
    }),

  cardTransfer: (transfer: CardTransferRequest, idempotencyKey: string) =>
    request<Payment>('/payments/card-transfers', {
      method: 'POST',
      body: transfer,
      headers: withKey(idempotencyKey),
    }),

  transferFee: (fromAccountId: string, amount: number) =>
    request<FeeQuote>(
      `/payments/transfers/fee?${new URLSearchParams({ fromAccountId, amount: String(amount) })}`,
    ),

  cardTransferFee: (fromCardId: string, amount: number) =>
    request<FeeQuote>(
      `/payments/card-transfers/fee?${new URLSearchParams({ fromCardId, amount: String(amount) })}`,
    ),
};
