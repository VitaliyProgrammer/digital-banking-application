import { useTranslation } from 'react-i18next';
import { cn } from '@/shared/lib/cn';
import { LANGUAGES, type Language } from '@/shared/i18n/i18n';
import styles from './LanguageSwitch.module.css';

const LABELS: Record<Language, string> = { uk: 'УКР', en: 'ENG' };

export function LanguageSwitch() {
  const { t, i18n } = useTranslation();

  return (
    <div className={styles.switch} role="group" aria-label={t('common.language')}>
      {LANGUAGES.map((language) => (
        <button
          key={language}
          type="button"
          lang={language}
          className={cn(styles.option, i18n.language === language && styles.active)}
          aria-pressed={i18n.language === language}
          onClick={() => void i18n.changeLanguage(language)}
        >
          {LABELS[language]}
        </button>
      ))}
    </div>
  );
}
