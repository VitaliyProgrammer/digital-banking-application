import { request } from '@/shared/api/http';
import type { Page } from '@/shared/api/page';

export interface Notification {
  id: string;
  eventType: string;
  title: string;
  body: string;
  createdAt: string;
  /** null while the notification is unread. */
  readAt: string | null;
}

export const notificationsApi = {
  latest: (size: number) => request<Page<Notification>>(`/notifications?page=0&size=${size}`),

  unreadCount: () => request<{ unread: number }>('/notifications/unread-count'),

  markRead: (id: string) => request<Notification>(`/notifications/${id}/read`, { method: 'POST' }),

  markAllRead: () => request<undefined>('/notifications/read-all', { method: 'POST' }),
};
