import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { en } from './locales/en';
import { uk } from './locales/uk';

export const LANGUAGES = ['uk', 'en'] as const;
export type Language = (typeof LANGUAGES)[number];

const STORAGE_KEY = 'taborovets.language';
const DEFAULT_LANGUAGE: Language = 'uk';

function isLanguage(value: string | null): value is Language {
  return LANGUAGES.some((language) => language === value);
}

function savedLanguage(): Language {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    return isLanguage(saved) ? saved : DEFAULT_LANGUAGE;
  } catch {
    return DEFAULT_LANGUAGE;
  }
}

void i18n.use(initReactI18next).init({
  resources: {
    uk: { translation: uk },
    en: { translation: en },
  },
  lng: savedLanguage(),
  fallbackLng: DEFAULT_LANGUAGE,
  interpolation: { escapeValue: false },
});

document.documentElement.lang = i18n.language;

i18n.on('languageChanged', (language) => {
  document.documentElement.lang = language;
  try {
    localStorage.setItem(STORAGE_KEY, language);
  } catch {
    // Private mode or blocked storage: the language still works until the tab is closed.
  }
});

export function currentLanguage(): Language {
  return isLanguage(i18n.language) ? i18n.language : DEFAULT_LANGUAGE;
}

export { i18n };
