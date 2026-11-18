import { useCallback, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAuth, useCurrentUser } from '@/features/auth/model/AuthContext';
import { useDismiss } from '@/shared/lib/useDismiss';
import { LanguageSwitch } from '@/shared/ui/LanguageSwitch/LanguageSwitch';
import styles from './UserMenu.module.css';

/** The avatar of the mockup: opens the client's name, the language and logging out. */
export function UserMenu() {
  const { t } = useTranslation();
  const { logout } = useAuth();
  const user = useCurrentUser();
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const close = useCallback(() => setOpen(false), []);
  useDismiss(containerRef, open, close);

  const initials = `${user.firstName.charAt(0)}${user.lastName.charAt(0)}`.toUpperCase();

  return (
    <div ref={containerRef} className={styles.container}>
      <button
        type="button"
        className={styles.avatar}
        aria-label={t('account.menu')}
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
      >
        {initials}
      </button>

      {open && (
        <div className={styles.panel}>
          <div className={styles.identity}>
            <span className={styles.name}>
              {user.firstName} {user.lastName}
            </span>
            <span className={styles.email}>{user.email}</span>
          </div>

          <div className={styles.row}>
            <span className={styles.label}>{t('common.language')}</span>
            <LanguageSwitch />
          </div>

          <button type="button" className={styles.logout} onClick={() => void logout()}>
            {t('common.logout')}
          </button>
        </div>
      )}
    </div>
  );
}
