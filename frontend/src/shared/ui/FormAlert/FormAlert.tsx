import type { ReactNode } from 'react';
import { cn } from '@/shared/lib/cn';
import styles from './FormAlert.module.css';

interface FormAlertProps {
  tone: 'error' | 'success';
  children: ReactNode;
}

export function FormAlert({ tone, children }: FormAlertProps) {
  return (
    <div className={cn(styles.alert, styles[tone])} role={tone === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  );
}
