import { useQuery } from '@tanstack/react-query';
import { savingsApi } from '../api/savingsApi';

export function useSavingsJars() {
  return useQuery({ queryKey: ['savings'], queryFn: savingsApi.list });
}
