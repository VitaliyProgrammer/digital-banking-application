import { cn } from '@/shared/lib/cn';
import styles from './Logo.module.css';

export type LogoVariant = 'sidebar' | 'card';

interface LogoProps {
  variant?: LogoVariant;
}

const BANK_NAME = 'Taborovets';
const BANK_SUBTITLE_LETTERS = [...'Universal Bank'];

export function Logo({ variant = 'sidebar' }: LogoProps) {
  return (
    <div className={cn(styles.logo, styles[variant])} role="img" aria-label="Taborovets Universal Bank">
      <div className={styles.mark} aria-hidden="true">
        <span className={cn(styles.bar, styles.barLow)} />
        <span className={cn(styles.bar, styles.barMid)} />
        <span className={cn(styles.bar, styles.barHigh)} />
      </div>

      <div className={styles.words} aria-hidden="true">
        <span className={cn(styles.name, 'text-trim')}>{BANK_NAME}</span>
        <span className={styles.subtitle}>
          {BANK_SUBTITLE_LETTERS.map((letter, index) => (
            <span key={index} className="text-trim">
              {letter}
            </span>
          ))}
        </span>
      </div>
    </div>
  );
}
