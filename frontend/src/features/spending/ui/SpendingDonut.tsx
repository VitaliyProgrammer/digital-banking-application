import type { SpendingCategory } from '../api/spendingApi';
import { CATEGORY_COLORS } from '../model/categoryColors';
import styles from './SpendingDonut.module.css';

/** A thin gap between the slices, as in the mockup, in percent of the circle. */
const GAP = 0.3;

interface Slice {
  category: SpendingCategory;
  amount: number;
}

function ringBackground(slices: Slice[], total: number): string {
  const [first] = slices;
  if (!first || total <= 0) {
    return 'var(--spending-empty)';
  }
  if (slices.length === 1) {
    return CATEGORY_COLORS[first.category];
  }

  let start = 0;
  const stops = slices.map(({ category, amount }) => {
    const end = start + (amount / total) * 100;
    const stop = `${CATEGORY_COLORS[category]} ${start + GAP}% ${end - GAP}%`;
    start = end;
    return stop;
  });
  const last = slices[slices.length - 1] ?? first;
  return `conic-gradient(from -90deg, ${CATEGORY_COLORS[last.category]} 0%, ${stops.join(', ')})`;
}

interface SpendingDonutProps {
  slices: Slice[];
  total: number;
  totalLabel: string;
  totalText: string;
  description: string;
}

export function SpendingDonut({ slices, total, totalLabel, totalText, description }: SpendingDonutProps) {
  return (
    <div
      className={styles.ring}
      style={{ background: ringBackground(slices, total) }}
      role="img"
      aria-label={description}
    >
      <div className={styles.hole}>
        <span className={styles.label}>{totalLabel}</span>
        <span className={styles.total}>{totalText}</span>
      </div>
    </div>
  );
}
