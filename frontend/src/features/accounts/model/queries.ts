import { useInfiniteQuery, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { Currency } from '@/shared/lib/money';
import { accountsApi } from '../api/accountsApi';

const STATEMENT_PAGE_SIZE = 20;

/** Every key starts with 'accounts', so one invalidation refreshes balances and statements together. */
export const accountKeys = {
  all: ['accounts'] as const,
  latest: (accountId: string, size: number) => ['accounts', accountId, 'latest', size] as const,
  statement: (accountId: string) => ['accounts', accountId, 'statement'] as const,
};

export function useAccounts() {
  return useQuery({ queryKey: accountKeys.all, queryFn: accountsApi.list });
}

/** The few latest operations, for the home screen. */
export function useLatestOperations(accountId: string | undefined, size: number) {
  return useQuery({
    queryKey: accountKeys.latest(accountId ?? '', size),
    queryFn: () => accountsApi.statement(accountId ?? '', 0, size),
    select: (page) => page.content,
    enabled: Boolean(accountId),
  });
}

/** The full statement, a page at a time: "show more" asks for the next page and keeps the ones already shown. */
export function useStatement(accountId: string | undefined) {
  return useInfiniteQuery({
    queryKey: accountKeys.statement(accountId ?? ''),
    queryFn: ({ pageParam }) => accountsApi.statement(accountId ?? '', pageParam, STATEMENT_PAGE_SIZE),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
    select: (data) => data.pages.flatMap((page) => page.content),
    enabled: Boolean(accountId),
  });
}

export function useOpenAccount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (currency: Currency) => accountsApi.open(currency),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: accountKeys.all }),
  });
}
