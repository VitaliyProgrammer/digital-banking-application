import { useCallback, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { cn } from '@/shared/lib/cn';
import { useDismiss } from '@/shared/lib/useDismiss';
import { useEventTime } from '@/shared/lib/useEventTime';
import { useNow } from '@/shared/lib/useNow';
import type { Notification } from '../api/notificationsApi';
import {
  useLatestNotifications,
  useMarkAllRead,
  useMarkRead,
  useUnreadCount,
} from '../model/useNotifications';
import styles from './NotificationBell.module.css';

export function NotificationBell() {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const close = useCallback(() => setOpen(false), []);
  useDismiss(containerRef, open, close);

  const { data: unread = 0 } = useUnreadCount();
  const latest = useLatestNotifications(open);
  const markRead = useMarkRead();
  const markAllRead = useMarkAllRead();
  const eventTime = useEventTime(useNow());

  const onItemClick = (notification: Notification) => {
    if (!notification.readAt) {
      markRead.mutate(notification.id);
    }
  };

  return (
    <div ref={containerRef} className={styles.container}>
      <button
        type="button"
        className={styles.bell}
        aria-label={unread > 0 ? t('notifications.bellUnread', { count: unread }) : t('notifications.bell')}
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
      >
        <BellIcon />
        {unread > 0 && (
          <span className={styles.count} aria-hidden="true">
            {unread > 99 ? '99+' : unread}
          </span>
        )}
      </button>

      {open && (
        <section className={styles.panel} aria-label={t('notifications.title')}>
          <header className={styles.panelHeader}>
            <h2 className={styles.panelTitle}>{t('notifications.title')}</h2>
            {unread > 0 && (
              <button
                type="button"
                className={styles.readAll}
                disabled={markAllRead.isPending}
                onClick={() => markAllRead.mutate()}
              >
                {t('notifications.readAll')}
              </button>
            )}
          </header>

          {latest.isError && <p className={styles.state}>{t('notifications.loadFailed')}</p>}
          {latest.isPending && <p className={styles.state}>{t('common.loading')}</p>}
          {latest.data?.length === 0 && <p className={styles.state}>{t('notifications.empty')}</p>}

          {latest.data && latest.data.length > 0 && (
            <ul className={styles.list}>
              {latest.data.map((notification) => (
                <li key={notification.id}>
                  <button
                    type="button"
                    className={cn(styles.item, !notification.readAt && styles.unread)}
                    onClick={() => onItemClick(notification)}
                  >
                    <span className={styles.dot} aria-hidden="true" />
                    <span className={styles.content}>
                      <span className={styles.itemTitle}>{notification.title}</span>
                      <span className={styles.itemBody}>{notification.body}</span>
                      <span className={styles.itemTime}>{eventTime(notification.createdAt)}</span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </div>
  );
}

function BellIcon() {
  return (
    <svg
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
      <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
    </svg>
  );
}
