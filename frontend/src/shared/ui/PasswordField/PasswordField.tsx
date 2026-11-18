import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { generatePassword } from '@/shared/lib/password';
import { TextField, type TextFieldProps } from '../TextField/TextField';
import styles from './PasswordField.module.css';

interface PasswordFieldProps extends Omit<TextFieldProps, 'type' | 'endAdornment'> {
  /** Shows "generate a strong password" and hands the generated one to the form. */
  onGenerate?: (password: string) => void;
}

export function PasswordField({ onGenerate, hint, ...fieldProps }: PasswordFieldProps) {
  const { t } = useTranslation();
  const [visible, setVisible] = useState(false);
  const [generated, setGenerated] = useState(false);

  const generate = () => {
    onGenerate?.(generatePassword());
    setVisible(true);
    setGenerated(true);
  };

  return (
    <div className={styles.wrapper}>
      <TextField
        type={visible ? 'text' : 'password'}
        hint={generated ? t('password.generated') : hint}
        endAdornment={
          <button
            type="button"
            className={styles.toggle}
            aria-label={visible ? t('password.hide') : t('password.show')}
            aria-pressed={visible}
            onClick={() => setVisible((current) => !current)}
          >
            {visible ? <EyeOffIcon /> : <EyeIcon />}
          </button>
        }
        {...fieldProps}
      />

      {onGenerate && (
        <button type="button" className={styles.generate} onClick={generate}>
          {t('password.generate')}
        </button>
      )}
    </div>
  );
}

function EyeIcon() {
  return (
    <svg
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7S2 12 2 12z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

function EyeOffIcon() {
  return (
    <svg
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M10.6 5.1A10.7 10.7 0 0 1 12 5c6.4 0 10 7 10 7a17.6 17.6 0 0 1-2.2 3.2M6.6 6.6C3.6 8.5 2 12 2 12s3.6 7 10 7a10 10 0 0 0 5.4-1.6" />
      <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2M2 2l20 20" />
    </svg>
  );
}
