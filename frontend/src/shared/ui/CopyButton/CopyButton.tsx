import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { cn } from '@/shared/lib/cn';
import { CheckIcon, CopyIcon } from '../icons';
import styles from './CopyButton.module.css';

const COPIED_SHOWN_MS = 2000;

interface CopyButtonProps {
  text: string;
  /** What is copied: "Скопіювати IBAN". A tooltip of the icon, or the button's text with `withText`. */
  label: string;
  withText?: boolean;
  className?: string;
}

export function CopyButton({ text, label, withText = false, className }: CopyButtonProps) {
  const { t } = useTranslation();
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!copied) {
      return;
    }
    const timer = window.setTimeout(() => setCopied(false), COPIED_SHOWN_MS);
    return () => window.clearTimeout(timer);
  }, [copied]);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
    } catch {
      // The browser refused the clipboard: the text stays on screen to be selected by hand.
    }
  };

  const shown = copied ? t('common.copied') : label;

  return (
    <>
      <button
        type="button"
        className={cn(styles.button, withText && styles.withText, copied && styles.copied, className)}
        onClick={() => void copy()}
        aria-label={withText ? undefined : label}
        title={withText ? undefined : shown}
      >
        {copied ? <CheckIcon /> : <CopyIcon />}
        {withText && <span>{shown}</span>}
      </button>
      {/* A screen reader hears "Скопійовано" too: a changed label alone is not announced. */}
      <span className={styles.announcement} aria-live="polite">
        {copied ? t('common.copied') : ''}
      </span>
    </>
  );
}
