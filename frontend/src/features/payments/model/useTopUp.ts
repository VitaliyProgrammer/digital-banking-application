import { paymentsApi } from '../api/paymentsApi';
import { usePaymentMutation } from './usePaymentMutation';

/** Tops up an account and refreshes its balance and statement. */
export function useTopUp() {
  return usePaymentMutation(paymentsApi.topUp);
}
