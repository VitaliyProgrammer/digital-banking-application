import type { CSSProperties, ReactNode } from 'react';
import { cn } from '@/shared/lib/cn';
import styles from './Pill.module.css';

interface PillProps {
  children: ReactNode;
  /** Background and text colour, as design tokens: { background: 'var(--pill-mobile)', … }. */
  colors: Pick<CSSProperties, 'background' | 'color'>;
  size?: 'regular' | 'large';
  className?: string;
}

export function Pill({ children, colors, size = 'regular', className }: PillProps) {
  return (
    <span className={cn(styles.pill, styles[size], className)} style={colors}>
      {children}
    </span>
  );
}
