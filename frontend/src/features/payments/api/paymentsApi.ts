import { request } from '@/shared/api/http';
import type { Currency } from '@/shared/lib/money';

export type PaymentStatus = 'PENDING' | 'PROCESSING' | 'UNKNOWN' | 'COMPLETED' | 'FAILED';

/** The part of the bank's payment the screens use so far; the rest arrives with the payments section. */
export interface Payment {
  id: string;
  status: PaymentStatus;
  currency: Currency;
  amount: number;
  createdAt: string;
}

export interface TopUpRequest {
  accountId: string;
  amount: number;
}

export const paymentsApi = {
  /** The key makes a repeated request return the first payment instead of paying twice. */
  topUp: (topUp: TopUpRequest, idempotencyKey: string) =>
    request<Payment>('/payments/top-up', {
      method: 'POST',
      body: topUp,
      headers: { 'Idempotency-Key': idempotencyKey },
    }),
};
