import { useCallback, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ChevronIcon } from '@/shared/ui/icons';
import { cn } from '@/shared/lib/cn';
import { useDismiss } from '@/shared/lib/useDismiss';
import { samePeriod, type SpendingPeriod } from '../model/periods';
import styles from './PeriodPicker.module.css';

export interface PeriodOption {
  period: SpendingPeriod;
  label: string;
  hint?: string;
}

interface PeriodPickerProps {
  kind: SpendingPeriod['kind'];
  onKindChange: (kind: SpendingPeriod['kind']) => void;
  options: PeriodOption[];
  selected: SpendingPeriod;
  onSelect: (period: SpendingPeriod) => void;
}

/** "Місяць / Рік" and the list of months or years, as in the approved mockup. */
export function PeriodPicker({ kind, onKindChange, options, selected, onSelect }: PeriodPickerProps) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const close = useCallback(() => setOpen(false), []);
  useDismiss(containerRef, open, close);

  const current = options.find((option) => samePeriod(option.period, selected));

  const changeKind = (next: SpendingPeriod['kind']) => {
    setOpen(false);
    onKindChange(next);
  };

  return (
    <div className={styles.picker}>
      <div className={styles.segment} role="group" aria-label={t('home.spending.period')}>
        {(['month', 'year'] as const).map((option) => (
          <button
            key={option}
            type="button"
            className={cn(styles.segmentButton, kind === option && styles.segmentActive)}
            aria-pressed={kind === option}
            onClick={() => changeKind(option)}
          >
            {t(`home.spending.${option}`)}
          </button>
        ))}
      </div>

      <div ref={containerRef} className={styles.dropdown}>
        <button
          type="button"
          className={styles.dropdownButton}
          aria-haspopup="listbox"
          aria-expanded={open}
          onClick={() => setOpen((value) => !value)}
        >
          {current?.label}
          <ChevronIcon className={cn(styles.chevron, open && styles.chevronUp)} />
        </button>

        {open && (
          <ul
            className={styles.list}
            role="listbox"
            aria-label={kind === 'month' ? t('home.spending.chooseMonth') : t('home.spending.chooseYear')}
          >
            {options.map((option) => {
              const isSelected = samePeriod(option.period, selected);
              return (
                <li key={option.label} role="option" aria-selected={isSelected}>
                  <button
                    type="button"
                    className={cn(styles.option, isSelected && styles.optionSelected)}
                    onClick={() => {
                      onSelect(option.period);
                      setOpen(false);
                    }}
                  >
                    <span>{option.label}</span>
                    {option.hint && <span className={styles.hint}>{option.hint}</span>}
                  </button>
                </li>
              );
            })}
          </ul>
        )}
      </div>
    </div>
  );
}
