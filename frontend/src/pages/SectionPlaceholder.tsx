import { useTranslation } from 'react-i18next';
import styles from './SectionPlaceholder.module.css';

/** A section of the menu that is not built yet: each one is replaced by its own screen in the next branches. */
export function SectionPlaceholder() {
  const { t } = useTranslation();

  return (
    <section className={styles.panel}>
      <p className={styles.text}>{t('section.soon')}</p>
    </section>
  );
}
