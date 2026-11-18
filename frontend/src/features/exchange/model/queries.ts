import { useQuery } from '@tanstack/react-query';
import { exchangeRatesApi } from '../api/exchangeRatesApi';

export function useExchangeRates() {
  return useQuery({ queryKey: ['exchange-rates'], queryFn: exchangeRatesApi.list, staleTime: 5 * 60_000 });
}
