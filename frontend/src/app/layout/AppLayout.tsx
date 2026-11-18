import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Outlet } from 'react-router';
import { NotificationBell } from '@/features/notifications/ui/NotificationBell';
import styles from './AppLayout.module.css';
import { PageHeading } from './PageHeading';
import { Sidebar } from './Sidebar';
import { UserMenu } from './UserMenu';

/** The frame of every screen of the internet bank: the menu on the left, the header and the screen itself. */
export function AppLayout() {
  const { t } = useTranslation();
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    if (!menuOpen) {
      return;
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setMenuOpen(false);
      }
    };
    document.addEventListener('keydown', closeOnEscape);
    return () => document.removeEventListener('keydown', closeOnEscape);
  }, [menuOpen]);

  return (
    <div className={styles.shell}>
      <Sidebar open={menuOpen} onNavigate={() => setMenuOpen(false)} />
      {menuOpen && <div className={styles.overlay} aria-hidden="true" onClick={() => setMenuOpen(false)} />}

      <div className={styles.content}>
        <header className={styles.header}>
          <button
            type="button"
            className={styles.menuButton}
            aria-label={menuOpen ? t('nav.close') : t('nav.open')}
            aria-expanded={menuOpen}
            aria-controls="sidebar"
            onClick={() => setMenuOpen((current) => !current)}
          >
            <MenuIcon />
          </button>

          <div className={styles.heading}>
            <PageHeading />
          </div>

          <div className={styles.actions}>
            <NotificationBell />
            <UserMenu />
          </div>
        </header>

        <main className={styles.main}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}

function MenuIcon() {
  return (
    <svg
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2.2"
      strokeLinecap="round"
      aria-hidden="true"
    >
      <path d="M4 7h16M4 12h16M4 17h16" />
    </svg>
  );
}
