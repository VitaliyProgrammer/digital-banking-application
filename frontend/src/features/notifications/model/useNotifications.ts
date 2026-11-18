import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { notificationsApi } from '../api/notificationsApi';

const LATEST_COUNT = 10;
const UNREAD_COUNT_REFRESH = 60_000;

const keys = {
  all: ['notifications'] as const,
  unreadCount: ['notifications', 'unread-count'] as const,
  latest: ['notifications', 'latest'] as const,
};

/** The number on the bell: asked once a minute, so a new payment notification shows up without a reload. */
export function useUnreadCount() {
  return useQuery({
    queryKey: keys.unreadCount,
    queryFn: notificationsApi.unreadCount,
    select: (data) => data.unread,
    refetchInterval: UNREAD_COUNT_REFRESH,
  });
}

export function useLatestNotifications(enabled: boolean) {
  return useQuery({
    queryKey: keys.latest,
    queryFn: () => notificationsApi.latest(LATEST_COUNT),
    select: (page) => page.content,
    enabled,
  });
}

export function useMarkRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: notificationsApi.markRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.all }),
  });
}

export function useMarkAllRead() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: notificationsApi.markAllRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: keys.all }),
  });
}
