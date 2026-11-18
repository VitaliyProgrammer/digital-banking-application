import { useQuery } from '@tanstack/react-query';
import { accountsApi } from '../api/accountsApi';

export const accountKeys = {
  all: ['accounts'] as const,
  statement: (accountId: string, size: number) => ['accounts', accountId, 'statement', size] as const,
};

export function useAccounts() {
  return useQuery({ queryKey: accountKeys.all, queryFn: accountsApi.list });
}

export function useStatement(accountId: string | undefined, size: number) {
  return useQuery({
    queryKey: accountKeys.statement(accountId ?? '', size),
    queryFn: () => accountsApi.statement(accountId ?? '', size),
    select: (page) => page.content,
    enabled: Boolean(accountId),
  });
}
