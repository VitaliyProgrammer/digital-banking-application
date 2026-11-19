import { useTranslation } from 'react-i18next';
import { currentLanguage } from '@/shared/i18n/i18n';
import { cn } from '@/shared/lib/cn';
import { formatMoney, type Currency } from '@/shared/lib/money';
import styles from './SourcePicker.module.css';

/** An account or a card the money can leave from, as the client recognises it. */
export interface TransferSource {
  id: string;
  /** "Гривневий рахунок", "Універсальна картка". */
  name: string;
  /** "UA21 … 6001", "•• 2048". */
  number: string;
  currency: Currency;
  balance: number;
  /** What may be spent: the balance, plus the credit limit for a card payment. */
  spendable: number;
}

interface SourcePickerProps {
  sources: TransferSource[];
  value: string;
  onChange: (sourceId: string) => void;
}

export function SourcePicker({ sources, value, onChange }: SourcePickerProps) {
  const { t } = useTranslation();
  const language = currentLanguage();

  return (
    <fieldset className={styles.group}>
      <legend className={styles.legend}>{t('transfers.from')}</legend>
      <div className={styles.options}>
        {sources.map((source) => (
          <label key={source.id} className={cn(styles.option, source.id === value && styles.chosen)}>
            <input
              type="radio"
              name="source"
              className={styles.radio}
              checked={source.id === value}
              onChange={() => onChange(source.id)}
            />
            <span className={styles.name}>{source.name}</span>
            <span className={styles.number}>{source.number}</span>
            <span className={styles.balance}>{formatMoney(source.balance, source.currency, language)}</span>
          </label>
        ))}
      </div>
    </fieldset>
  );
}
