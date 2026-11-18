import { useTranslation } from 'react-i18next';
import { NavLink } from 'react-router';
import { cn } from '@/shared/lib/cn';
import { Logo } from '@/shared/ui/Logo/Logo';
import { NAVIGATION } from './navigation';
import styles from './Sidebar.module.css';

interface SidebarProps {
  open: boolean;
  onNavigate: () => void;
}

export function Sidebar({ open, onNavigate }: SidebarProps) {
  const { t } = useTranslation();

  return (
    <aside id="sidebar" className={cn(styles.sidebar, open && styles.open)}>
      <div className={styles.inner}>
        <div className={styles.logo}>
          <Logo />
        </div>

        <nav aria-label={t('nav.label')}>
          <ul className={styles.list}>
            {NAVIGATION.map((item) => (
              <li key={item.path}>
                <NavLink
                  to={item.path}
                  end={item.path === '/'}
                  className={({ isActive }) => cn(styles.link, isActive && styles.active)}
                  onClick={onNavigate}
                >
                  {t(`nav.${item.label}`)}
                  {item.tag && <span className={styles.tag}>{t(`nav.${item.tag}`)}</span>}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      </div>
    </aside>
  );
}
