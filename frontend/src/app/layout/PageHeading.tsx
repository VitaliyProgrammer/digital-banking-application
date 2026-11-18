import { useTranslation } from 'react-i18next';
import { useMatches } from 'react-router';
import { useCurrentUser } from '@/features/auth/model/AuthContext';
import { currentLanguage } from '@/shared/i18n/i18n';
import { formatLongDate } from '@/shared/lib/kyivTime';
import { useNow } from '@/shared/lib/useNow';
import type { NavKey } from './navigation';
import styles from './PageHeading.module.css';
import { useGreeting } from './useGreeting';

/** What a route shows in the header: a section title, or nothing for the home page, which greets the client. */
export interface RouteHandle {
  title?: NavKey;
}

export function PageHeading() {
  const { t } = useTranslation();
  const user = useCurrentUser();
  const now = useNow();
  const greeting = useGreeting(user.firstName, now);

  const title = useMatches()
    .map((match) => (match.handle as RouteHandle | undefined)?.title)
    .findLast(Boolean);

  return (
    <div className={styles.heading}>
      <p className={styles.date}>{formatLongDate(now, currentLanguage())}</p>
      <h1 className={styles.title}>{title ? t(`nav.${title}`) : greeting}</h1>
    </div>
  );
}
