import { useQuery } from '@tanstack/react-query';
import { cashbackApi } from '../api/cashbackApi';

export function useCurrentCashback() {
  return useQuery({ queryKey: ['cashback', 'current'], queryFn: cashbackApi.currentMonth });
}
