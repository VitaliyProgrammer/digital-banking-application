import { useQuery } from '@tanstack/react-query';
import { creditLineApi } from '../api/creditLineApi';

export function useCreditLine() {
  return useQuery({ queryKey: ['credit-line'], queryFn: creditLineApi.find });
}
