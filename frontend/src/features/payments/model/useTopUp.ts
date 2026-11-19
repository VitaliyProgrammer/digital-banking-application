import { useMutation, useQueryClient } from '@tanstack/react-query';
import { accountKeys } from '@/features/accounts/model/queries';
import { ApiError, NETWORK_ERROR_CODE } from '@/shared/api/ApiError';
import { useIdempotencyKey } from '@/shared/lib/useIdempotencyKey';
import { paymentsApi, type TopUpRequest } from '../api/paymentsApi';

/**
 * Tops up an account and refreshes its balance and statement. When the answer is lost on the way,
 * the client's retry carries the same key, so the money is credited once.
 */
export function useTopUp() {
  const queryClient = useQueryClient();
  const { keyFor, settle } = useIdempotencyKey();

  return useMutation({
    mutationFn: (topUp: TopUpRequest) => paymentsApi.topUp(topUp, keyFor(topUp)),
    onSuccess: () => {
      settle();
      return queryClient.invalidateQueries({ queryKey: accountKeys.all });
    },
    onError: (error) => {
      // The bank answered, so it decided: only a lost answer is retried with the same key.
      if (error instanceof ApiError && error.code !== NETWORK_ERROR_CODE) {
        settle();
      }
    },
  });
}
