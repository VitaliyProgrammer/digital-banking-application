import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { LanguageSwitch } from '@/shared/ui/LanguageSwitch/LanguageSwitch';
import { Logo } from '@/shared/ui/Logo/Logo';
import styles from './AuthLayout.module.css';

interface AuthLayoutProps {
  title: string;
  subtitle?: string;
  children: ReactNode;
  footer?: ReactNode;
}

const FEATURES = ['accounts', 'payments', 'savings'] as const;

/** Every page before the login: the bank on the left, the form on the right (stacked on a phone). */
export function AuthLayout({ title, subtitle, children, footer }: AuthLayoutProps) {
  const { t } = useTranslation();

  return (
    <div className={styles.page}>
      <header className={styles.topBar}>
        <div className={styles.compactLogo}>
          <Logo />
        </div>
        <LanguageSwitch />
      </header>

      <div className={styles.layout}>
        <aside className={styles.brand}>
          <Logo />
          <p className={styles.brandTitle}>{t('auth.brand.title')}</p>
          <ul className={styles.features}>
            {FEATURES.map((feature) => (
              <li key={feature} className={styles.feature}>
                <CheckIcon />
                <span>{t(`auth.brand.features.${feature}`)}</span>
              </li>
            ))}
          </ul>
        </aside>

        <main className={styles.panel}>
          <div className={styles.heading}>
            <h1 className={styles.title}>{title}</h1>
            {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
          </div>
          {children}
          {footer && <div className={styles.footer}>{footer}</div>}
        </main>
      </div>
    </div>
  );
}

function CheckIcon() {
  return (
    <span className={styles.check} aria-hidden="true">
      <svg
        width="14"
        height="14"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="3"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d="M5 12.5l4.5 4.5L19 7.5" />
      </svg>
    </span>
  );
}
