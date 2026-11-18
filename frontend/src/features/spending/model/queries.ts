import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { spendingApi } from '../api/spendingApi';

export function useSpending(from: string, to: string) {
  return useQuery({
    queryKey: ['spending', from, to],
    queryFn: () => spendingApi.get(from, to),
    // While another month loads, the chart keeps the previous one instead of flashing empty.
    placeholderData: keepPreviousData,
  });
}
