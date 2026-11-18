import { useTranslation } from 'react-i18next';
import { Logo } from '../Logo/Logo';
import styles from './PageLoader.module.css';

export function PageLoader() {
  const { t } = useTranslation();

  return (
    <div className={styles.loader} role="status" aria-label={t('common.loading')}>
      <div className={styles.pulse}>
        <Logo />
      </div>
    </div>
  );
}
