import type { ReactNode } from 'react';
import { cn } from '@/shared/lib/cn';
import styles from './SegmentedControl.module.css';

export interface Segment<T extends string> {
  value: T;
  label: ReactNode;
}

interface SegmentedControlProps<T extends string> {
  /** The radio group's name: unique on the page. */
  name: string;
  segments: readonly Segment<T>[];
  value: T;
  onChange: (value: T) => void;
  /** Needed when no legend names the group, e.g. "Куди переказати". */
  'aria-label'?: string;
  className?: string;
}

/**
 * One choice of a few, side by side. Real radios underneath, so arrows, Tab and screen readers
 * work the way they do with any radio group.
 */
export function SegmentedControl<T extends string>({
  name,
  segments,
  value,
  onChange,
  'aria-label': ariaLabel,
  className,
}: SegmentedControlProps<T>) {
  return (
    // Inside a fieldset the legend already names the group; on its own the control names itself.
    <div
      className={cn(styles.segments, className)}
      role={ariaLabel ? 'radiogroup' : undefined}
      aria-label={ariaLabel}
    >
      {segments.map((segment) => (
        <label key={segment.value} className={cn(styles.segment, segment.value === value && styles.chosen)}>
          <input
            type="radio"
            name={name}
            className={styles.radio}
            checked={segment.value === value}
            onChange={() => onChange(segment.value)}
          />
          {segment.label}
        </label>
      ))}
    </div>
  );
}
