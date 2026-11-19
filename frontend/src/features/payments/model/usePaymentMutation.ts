import { useMutation, useQueryClient } from '@tanstack/react-query';
import { accountKeys } from '@/features/accounts/model/queries';
import { ApiError, NETWORK_ERROR_CODE } from '@/shared/api/ApiError';
import { useIdempotencyKey } from '@/shared/lib/useIdempotencyKey';
import type { Payment } from '../api/paymentsApi';

/** Every money movement changes balances, statements and "where the money went". */
const AFFECTED_BY_PAYMENTS = [accountKeys.all, ['spending']] as const;

/**
 * Sends a payment once. When the answer is lost on the way, the client's retry carries the same key,
 * so the bank pays once; any answer of the bank closes the payment, and the next one gets a new key.
 */
export function usePaymentMutation<T>(send: (payment: T, idempotencyKey: string) => Promise<Payment>) {
  const queryClient = useQueryClient();
  const { keyFor, settle } = useIdempotencyKey();

  return useMutation({
    mutationFn: (payment: T) => send(payment, keyFor(payment)),
    onSuccess: () => {
      settle();
      return Promise.all(AFFECTED_BY_PAYMENTS.map((queryKey) => queryClient.invalidateQueries({ queryKey })));
    },
    onError: (error) => {
      // The bank answered, so it decided: only a lost answer is retried with the same key.
      if (error instanceof ApiError && error.code !== NETWORK_ERROR_CODE) {
        settle();
      }
    },
  });
}
