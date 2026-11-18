import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { currentLanguage } from '@/shared/i18n/i18n';
import { isEvening } from '@/shared/lib/kyivTime';
import { toVocative } from '@/shared/lib/vocative';

/**
 * "Добрий день, Іване!" until 17:00 in Kyiv, "Добрий вечір, Іване!" from 17:00.
 * In Ukrainian the name is in the vocative case; until it is ready the name is shown as it is.
 */
export function useGreeting(firstName: string, now: Date): string {
  const { t } = useTranslation();
  const language = currentLanguage();
  const [vocative, setVocative] = useState<{ for: string; name: string } | null>(null);

  useEffect(() => {
    if (language !== 'uk') {
      return;
    }
    let cancelled = false;
    void toVocative(firstName).then((name) => {
      if (!cancelled) {
        setVocative({ for: firstName, name });
      }
    });
    return () => {
      cancelled = true;
    };
  }, [firstName, language]);

  const name = language === 'uk' && vocative?.for === firstName ? vocative.name : firstName;
  return isEvening(now) ? t('greeting.evening', { name }) : t('greeting.day', { name });
}
