import { useTranslation } from 'react-i18next';
import { useAuth, useCurrentUser } from '@/features/auth/model/AuthContext';
import { Button } from '@/shared/ui/Button/Button';
import { Logo } from '@/shared/ui/Logo/Logo';
import styles from './HomePage.module.css';

/** A placeholder until the approved home screen is built in the next branches. */
export function HomePage() {
  const { t } = useTranslation();
  const { logout } = useAuth();
  const user = useCurrentUser();

  return (
    <main className={styles.page}>
      <Logo />
      <p className={styles.text}>{t('home.signedInAs', { name: `${user.firstName} ${user.lastName}` })}</p>
      <Button variant="soft" onClick={() => void logout()}>
        {t('common.logout')}
      </Button>
    </main>
  );
}
