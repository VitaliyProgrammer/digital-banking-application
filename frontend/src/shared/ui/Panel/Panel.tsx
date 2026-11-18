import type { HTMLAttributes, ReactNode } from 'react';
import { cn } from '@/shared/lib/cn';
import styles from './Panel.module.css';

interface PanelProps extends HTMLAttributes<HTMLElement> {
  children?: ReactNode;
}

/** The white rounded block of the mockup that every section of a screen sits in. */
export function Panel({ className, children, ...rest }: PanelProps) {
  return (
    <section className={cn(styles.panel, className)} {...rest}>
      {children}
    </section>
  );
}

export function PanelTitle({ children, className }: { children: ReactNode; className?: string }) {
  return <h2 className={cn(styles.title, className)}>{children}</h2>;
}

/** What a panel shows when its data could not be loaded. */
export function PanelMessage({ children }: { children: ReactNode }) {
  return <p className={styles.message}>{children}</p>;
}
